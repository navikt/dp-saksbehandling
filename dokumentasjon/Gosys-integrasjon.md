# Gosys-integrasjon i DP-sak — Skisse og avklaringspunkter

> **Status:** Utredningsfase — klar for stakeholder-input  
> **Team:** STSB (Saksbehandling dagpenger)  
> **Dato:** Mai 2026

---

## 🎯 Problemet vi løser

Dagpenger-saksbehandlere må i dag **manuelt sjekke Gosys** for å se om det finnes oppgaver knyttet til sine brukere — henvendelser fra NKS, purringer, kontakt bruker-oppgaver, oppgaver fra lokalkontor, osv.

**Mål:** Gosys-oppgaver for dagpengerbrukere skal dukke opp automatisk i DP-sak, slik at saksbehandler kan håndtere dem uten å bytte system.

---

## 🏗️ Foreslått løsning — arkitekturskisse

```
┌──────────────────────────────────────────────────────────────────────┐
│                         GOSYS / Oppgave                              │
│  (eies av Team Oppgavehåndtering)                                    │
│                                                                      │
│  Kafka-topic: publiserer hendelser    REST API v2: full oppgavedata  │
│  når oppgaver opprettes/endres        (beskrivelse, dokumenter, etc) │
└──────────┬───────────────────────────────────────────┬───────────────┘
           │ Kafka (tynn hendelse)                     │ REST (rik data)
           ▼                                           │
┌─────────────────────────┐                            │
│   dp-gosys-oppgave      │                            │
│   (NY stateless app)    │                            │
│                         │                            │
│  • Filtrerer tema=DAG   │◄───── PATCH ──────────────►│
│  • Videresender til     │  (marker som "vår" +       │
│    Rapids & Rivers      │   ferdigstill)             │
└──────────┬──────────────┘                            │
           │ Rapids-melding                             │
           ▼                                           │
┌──────────────────────────────────────────────────────┤
│   dp-saksbehandling                                  │
│   (eksisterende app)                                 │
│                                                      │
│  • Sjekker: er personen i dp-sak?                    │
│  • JA → oppretter Oppfølgings-oppgave               │
│  • NEI → ignorerer                                   │
│                                                      │
│  Saksbehandler åpner oppgaven:                       │
│  • Henter fersk data fra Gosys ──────────────────────┤ REST on-demand
│  • Ser beskrivelse, type, frist                      │
│  • Ferdigstiller → lukker i Gosys automatisk         │
└──────────────────────────────────────────────────────┘
```

### Flyten steg for steg

| Steg | Hva skjer | System |
|------|-----------|--------|
| 1 | NKS/lokalkontor/annet oppretter oppgave i Gosys med tema DAG | Gosys |
| 2 | Gosys publiserer tynn Kafka-hendelse | Gosys |
| 3 | dp-gosys-oppgave filtrerer tema=DAG, videresender til Rapids | dp-gosys-oppgave |
| 4 | dp-saksbehandling sjekker: har vi denne personen? | dp-saksbehandling |
| 5 | Hvis ja: oppretter oppfølgings-oppgave i dp-sak | dp-saksbehandling |
| 6 | dp-gosys-oppgave markerer Gosys-oppgaven som "behandles i DP-SAK" | dp-gosys-oppgave |
| 7 | Saksbehandler ser oppgaven i dp-sak sin oppgaveliste | Frontend |
| 8 | Saksbehandler åpner → fersk data hentes fra Gosys REST | dp-saksbehandling |
| 9 | Saksbehandler vurderer og ferdigstiller | Frontend |
| 10 | dp-gosys-oppgave ferdigstiller oppgaven i Gosys | dp-gosys-oppgave |

---

## 📊 Hva vi vet — analyser gjennomført

| Analyse | Resultat |
|---------|---------|
| Gosys Kafka-format | **Tynn hendelse** — inneholder ID, type, frist, person, men IKKE beskrivelse eller dokumenter |
| Gosys REST API v2 | **Rik data** — inneholder alt inkl. beskrivelse, kommentarer, dokumentreferanser |
| FP-sak (foreldrepenger) | Henter alltid fersk data via REST on-demand. Viser oppgaver inline med dokumenter |
| K9-sak (pleiepenger) | Bruker også REST on-demand. Blokkerer vedtak hvis åpne Gosys-oppgaver finnes |
| Kelvin (AAP) | Eget oppgavesystem, integrerer ikke mot Gosys for lesing |
| dp-mottak | Kun *utgående* — oppretter journalføringsoppgaver i Gosys. Ingen overlapp |
| DAG oppgavetyper | 17 typer, 9 høy prioritet for MVP |
| `behandlesAvApplikasjon` | Felt i Gosys som markerer at en oppgave håndteres av eksternt fagsystem |

### 9 oppgavetyper i MVP

| Kode | Beskrivelse | Typisk kilde |
|------|-------------|-------------|
| BEH_HENV | Behandle henvendelse | NKS |
| VURD_HENV | Vurdere henvendelse | NKS |
| KONT_BRUK | Kontakte bruker | NKS / saksbehandler |
| VUR_KONS_YTE | Vurdere konsekvens for ytelse | Internt |
| VUR_SVAR | Vurdere svar | Oppfølging |
| INNH_DOK | Innhente dokumentasjon | Saksbehandler |
| SVAR_IK_MOT | Svar ikke mottatt | Automatisk |
| RETUR | Returoppgave | Annen enhet |
| ETTERSEND_MOTT | Ettersendelse mottatt | Automatisk |

---

## ⚠️ Avklaringspunkter

### 🔴 Krever saksbehandler/design-input

#### 1. Hva trenger saksbehandler å se?
Når saksbehandler åpner en Gosys-oppgave i dp-sak, hva er essensielt?

| Data | Tilgjengelig via | Prioritet? |
|------|-----------------|-----------|
| Oppgavetype (f.eks. "Behandle henvendelse") | Kafka + REST | |
| Frist | Kafka + REST | |
| Beskrivelse (fritekst fra den som opprettet) | Kun REST | |
| Kommentarer | Kun REST | |
| Tilknyttede dokumenter (fra Joark) | Kun REST + Joark | |
| Hvem som opprettet | Kun REST | |
| Deeplink til Gosys | Kan konstrueres | |

**Spørsmål:**
- Er beskrivelse + type + frist nok for de fleste oppgaver?
- Trenger vi dokumentvisning, eller holder en lenke til Gosys?
- Skal vi vise kommentarhistorikk?

#### 2. Hvordan ferdigstiller saksbehandler?
I dag har oppfølgingsoppgaver 5 ferdigstill-varianter:

| Variant | Beskrivelse |
|---------|-------------|
| AVSLUTT | Bare lukk oppgaven |
| OPPRETT_KLAGE | Oppgaven resulterer i en klage |
| OPPRETT_MANUELL_BEHANDLING | Oppgaven krever ny manuell behandling |
| OPPRETT_REVURDERING_BEHANDLING | Oppgaven krever revurdering |
| OPPRETT_OPPFOLGING | Oppgaven resulterer i ny oppfølging |

**Spørsmål:**
- Er disse variantene dekkende for Gosys-oppgaver?
- Trenger vi nye varianter?
- Skal saksbehandler kunne skrive en vurdering/kommentar ved ferdigstilling?

#### 3. UX-design
- Skal Gosys-oppgaver vises i **samme oppgaveliste** som andre oppfølgingsoppgaver, eller i en **egen seksjon**?
- Trenger vi visuell markering (emneknagg/ikon) for at det er en Gosys-oppgave?
- Hvordan viser vi at oppgaven faktisk kommer fra Gosys (transparens)?

---

### 🟡 Krever avklaring med andre team

#### 4. Avtale med Team Oppgavehåndtering

**Spørsmål vi trenger svar på:**

| # | Spørsmål | Kontekst | Vår antagelse |
|---|----------|----------|---------------|
| 1 | App-kode for `behandlesAvApplikasjon` | Vi trenger en registrert kode (f.eks. "DP-SAK") | Registreres hos dere |
| 2 | Kafka topic-navn | Trenger lesetilgang til Gosys sin oppgavehendelse-topic i dev og prod | `oppgavehandtering.oppgave-hendelse-v1`? |
| 3 | Konsekvenser av `behandlesAvApplikasjon` | Når vi markerer en oppgave, hva skjer i Gosys-UI? Begrenses redigering? | Ja, basert på kildekoden (`Begrensning` i v2 DTO) |
| 4 | Ferdigstilling via API | Vi ønsker å PATCH status→FERDIGSTILT når saksbehandler ferdigstiller i dp-sak. Er dette greit? | Ja — FP gjør det allerede |
| 5 | `endretAvEnhetsnr` ved ferdigstilling | Hvilken enhetsnr bruker vi? Saksbehandlers enhet, eller en fast «dp-sak»-enhet? | Saksbehandlers enhet |
| 6 | Optimistic locking ved ferdigstilling | Vi lagrer `versjon` fra Kafka-hendelsen ved opprettelse. Kan oppgaven ha fått ny versjon mellom opprettelse og ferdigstilling? | Ja, vi henter fersk versjon via GET først |
| 7 | Feilregistrering | Kan saksbehandler feilregistrere via dp-sak? (dvs. PATCH status→FEILREGISTRERT) | Ikke i MVP, men mulig teknisk |
| 8 | OPPGAVE_OPPRETTET-hendelsen | Inneholder den tilstrekkelig info til å avgjøre om vi skal plukke opp oppgaven? (ident, tema, oppgavetype) | Ja, basert på `OppgaveKafkaAivenRecord` |

**Kontekst for Team Oppgavehåndtering:**
- Vi bygger dagpenger-saksbehandling i nytt fagsystem (dp-sak)
- Saksbehandlere sjekker i dag Gosys manuelt for å se henvendelser
- Vi vil automatisk plukke opp DAG-oppgaver for personer vi allerede behandler
- Modell: som FP — fagsystem tar over oppgaven, ferdigstiller via API
- Volumet er pt svært lavt (<100 personer i systemet)

#### 5. Hva skjer når noen lukker oppgaven i Gosys direkte?
Scenario: Vi har laget en oppfølging i dp-sak, men noen ferdigstiller oppgaven direkte i Gosys.

**Foreslått løsning:** Saksbehandler ser oppdatert status neste gang de åpner oppgaven (fordi vi henter fersk data via REST). Kan evt. avbryte oppfølgingen da.

**Alternativ:** Lytte på FERDIGSTILT-hendelser fra Gosys Kafka og auto-lukke. Mer automatisering, men mer kompleksitet.

**Spørsmål:** Hva foretrekkes for MVP?

---

### 🟢 Allerede besluttet

| Beslutning | Begrunnelse |
|------------|-------------|
| Separat bridge-app (dp-gosys-oppgave) | Isolerer Gosys-avhengigheten, dp-saksbehandling forblir ren |
| dp-saksbehandling eier eierskapssjekken | Har allerede person-data i DB |
| Kun nye oppgaver (ingen backfill) | Enklere MVP, gradvis onboarding |
| Always-fresh REST on-demand | Unngår stale data, løser berikelsesproblemet |
| Gjenbruk oppfølgings-mønsteret | Infrastrukturen finnes allerede (tilstandsmaskin, frist, emneknagger) |
| Gosys v2 API | Anbefalt av Team Oppgavehåndtering, v1 fases ut |
| Eierskapssjekk = person har Sak | `finnSakHistorikk(ident) != null` — enkel eksistenssjekk |
| Idempotens — skip for MVP | Duplikater er teoretiske, konsekvenser håndterbare |
| REST-klient i dp-saksbehandling | Kaller Gosys direkte, ikke via proxy |
| Feilhåndtering = la det feile | Oppgavelista fungerer alltid (lokal data), detaljvisning feiler med melding + deeplink |
| Sak-knytning = personnivå | Oppfølging knyttes til person, ikke sak (som eksisterende oppfølginger) |
| Ingen feature toggle | <100 brukere, deploy = on, redeploy = off |
| UX = konvolutt/strukturertData | Backend leverer Gosys-data via strukturertData. Frontend utvides iterativt |
| Gosys-data aldri cachet | Hentes alltid fersk fra Gosys REST når frontend spør |
| Kun OPPGAVE_OPPRETTET for MVP | Saksbehandler ser oppdatert status via fersk REST-data |

---

## 🗺️ Scope og avgrensninger

### I scope (MVP)
- ✅ Lytte på Gosys Kafka (tema=DAG, 9 oppgavetyper)
- ✅ Opprette oppfølging i dp-sak for kjente personer
- ✅ Markere oppgave som "behandles i DP-SAK" i Gosys
- ✅ Vise oppgavedata for saksbehandler (fersk fra REST)
- ✅ Ferdigstille oppgave i dp-sak → automatisk lukke i Gosys

### Utenfor scope (MVP)
- ❌ Opprette oppgaver i Gosys fra dp-sak
- ❌ Backfill av eksisterende Gosys-oppgaver
- ❌ Toveis kommentarsynkronisering
- ❌ Håndtering av OPPGAVE_ENDRET-hendelser (kan komme i v2)
- ❌ Integrasjon med Salesforce direkte (det er Gosys som er kontaktpunkt)

---

## 🔮 Veikart

| Fase | Innhold | Avhengigheter |
|------|---------|---------------|
| **MVP** | Lytt → opprett oppfølging → vis → ferdigstill | Saksbehandler-input, Gosys-avtale |
| **v2** | Auto-lukk ved Gosys-ferdigstilling, flere oppgavetyper | Erfaring fra MVP |
| **v3** | Dokumentvisning, kommentarer, deeplinks | Design-beslutning |
| **Fremtid** | Opprette oppgaver i Gosys, statusdeling til NKS/Salesforce | Produkt-prioritering |

---

## 👥 Hvem trenger vi input fra?

| Rolle | Spørsmål | Prioritet |
|-------|----------|-----------|
| **Saksbehandlere** | Hva trenger de å se? Hvordan ferdigstiller de? (pkt 1-2) | 🔴 Blokkerer |
| **Designere** | UX for Gosys-oppgaver i oppgavelisten (pkt 3) | 🔴 Blokkerer |
| **Produkteier** | Scope/prioritering, MVP-avgrensning + valg av ambisjonsnivå (se under) | 🟡 Viktig |
| **Team Oppgavehåndtering** | App-kode, topic-navn, konsekvenser (pkt 4) | 🟡 Viktig |
| **Frontend-utviklere** | Teknisk gjennomførbarhet av lazy-load fra Gosys | 🟢 Kan starte |
| **Backend-utviklere** | Review av arkitekturskisse | 🟢 Kan starte |

---

## ⚖️ Kompleksitetsanalyse — tre ambisjonsnivåer

### Alternativ A: «Metadata-only» — bare marker `gjelder` i Gosys

> Enkleste mulige integrasjon. Ingen endring i dp-sak.

**Hva:** Ny liten app lytter på Gosys Kafka, filtrerer tema=DAG, PATCH-er `gjelder`-feltet med f.eks. dagpenger-saksnummer slik at saksbehandler ser koblingen *i Gosys*.

```
Gosys Kafka → dp-gosys-oppgave → sjekk dp-sak (person finnes?) → PATCH gjelder i Gosys
```

| Dimensjon | Vurdering |
|-----------|-----------|
| Ny app | 1 liten (dp-gosys-oppgave), ~3 filer kode |
| Endringer i dp-saksbehandling | Ingen |
| Endringer i frontend | Ingen |
| Avhengigheter | Gosys Kafka + REST v2 |
| Nye Kafka-events | 0 (intern Rapids ikke nødvendig) |
| Database | Ingen |
| Design/UX | Ikke nødvendig |
| Auth | Azure CC → Gosys |
| Test-scope | Kafka consumer + REST mock |
| **Estimert kompleksitet** | 🟢 **Lav** |

**Gevinst:** Saksbehandler ser i Gosys at oppgaven er knyttet til en dagpengesak. Slipper å lete.  
**Begrensning:** Saksbehandler må fortsatt **jobbe i Gosys**. Ingen oppgave i dp-sak. Ingen automatisk ferdigstilling.

---

### Alternativ B: «Full integrasjon» — oppfølging i dp-sak (foreslått arkitektur)

> Gosys-oppgaver blir oppfølgingsoppgaver i dp-sak. Saksbehandler jobber i dp-sak.

```
Gosys Kafka → dp-gosys-oppgave → Rapids → dp-saksbehandling (oppfølging)
                    ↑                              ↓
              PATCH Gosys ←──── Rapids events ──── ferdigstill
              
Saksbehandler åpner oppgave → REST on-demand til Gosys → fersk data
```

| Dimensjon | Vurdering |
|-----------|-----------|
| Ny app | 1 (dp-gosys-oppgave), ~6-8 filer kode |
| Endringer i dp-saksbehandling | Ny mottak + Gosys REST-klient + event-publisering |
| Endringer i frontend | Ny visning for Gosys-oppgavedata (beskrivelse, type, frist) |
| Avhengigheter | Gosys Kafka + REST v2, Joark (evt.) |
| Nye Kafka-events | 3 (`gosys_dag_oppgave`, `oppfolging_oppgave_opprettet`, `oppfolging_oppgave_ferdigstilt`) |
| Database | Ingen ny (gjenbruker oppfølging-tabeller) |
| Design/UX | 🔴 Krever design — hva ser saksbehandler? |
| Auth | Azure CC → Gosys + intern Rapids |
| Test-scope | Kafka consumer, REST mock, mottak-test, mediator-test, frontend-test |
| **Estimert kompleksitet** | 🟡 **Medium** |

**Gevinst:** Saksbehandler jobber i **ett system**. Automatisk ferdigstilling i Gosys. Full sporbarhet.  
**Begrensning:** Krever design-avklaring, frontend-arbeid, og koordinering med Team Oppgavehåndtering.

---

### Sammenligning

```
                    Verdi for saksbehandler
                    ▲
                    │
         B+ ●──────┤  + dokumentvisning, kommentarer
                    │    (fremtidig utvidelse av B)
                    │
          B ●──────┤  Jobber i ett system
                    │  Automatisk Gosys-håndtering
                    │
                    │
          A ●──────┤  Ser kobling i Gosys
                    │  Må fortsatt bytte system
                    │
                    └──────────────────────────► Kompleksitet/tid
                    A          B          B+
```

| | Alt A: Metadata | Alt B: Full integrasjon |
|---|---|---|
| **Nye apper** | 1 liten | 1 liten |
| **Backend-endringer** | ~0 | Medium |
| **Frontend-endringer** | 0 | Medium |
| **Design nødvendig** | Nei | Ja |
| **Avhengighet andre team** | Lav (kun topic) | Medium (topic + app-kode) |
| **Risiko** | Lav | Medium |
| **Saksbehandler i dp-sak** | ❌ Nei, jobber i Gosys | ✅ Ja |
| **Kan bygge inkrementelt** | ✅ Startes nå | ⚠️ Trenger design først |

> **B+** er ikke et eget alternativ, men en naturlig utvidelse av B over tid (dokumentvisning fra Joark, kommentarsynkronisering). Krever ingen arkitekturendring — bare nye features.

---

### 💡 Anbefaling: Inkrementell tilnærming

```
Steg 1:  Alt A (metadata-only)     ← kan starte umiddelbart, ingen blokkere
    │
    │     Parallelt: design-avklaring med saksbehandlere
    │
    ▼
Steg 2:  Alt B (full integrasjon)  ← starter når design er klart
    │                                  gjenbruker infrastruktur fra A
    │     Parallelt: saksbehandler-feedback
    │
    ▼
Steg 3:  B+ (dokumenter m.m.)     ← basert på erfaring og behov fra B
```

**Fordelen:** Alt A gir umiddelbar verdi og bygger infrastrukturen (Kafka-consumer, Gosys REST-klient, eierskapssjekk) som B trenger. Alt A er ikke bortkastet arbeid — det er **fundament for B**.

**Nøkkelspørsmål til produkteier:**
1. Er «ser koblingen i Gosys» (Alt A) verdifullt nok som første steg?
2. Eller er det kun «jobber i dp-sak» (Alt B) som gir reell verdi?
3. Kan vi leve med at Alt B tar lengre tid fordi det krever design-avklaring?
