# Generell Oppgave

Generell oppgave er en fleksibel oppgavetype som lar saksbehandlere håndtere arbeid som ikke passer inn i eksisterende behandlingsflyter (søknad, meldekort, klage etc.). Oppgaver kan opprettes automatisk via Kafka-hendelser fra andre systemer, eller manuelt av saksbehandler via REST API.

## Typiske brukstilfeller

- PDL-hendelser (adresseendring, dødsfall) som krever manuell oppfølging
- Meldekort som trenger korrigering
- Henvendelser fra NKS/veiledere
- Bro fra Gosys
- Generelt arbeid som ikke har en tilhørende søknad eller vedtak

## Arkitektur

### Datamodell

```
Person ← GenerellOppgave ──→ Behandling (UtløstAvType.GENERELL)
                         └──→ Oppgave (emneknagger = { aarsak })
```

`GenerellOppgave` er et selvstendig domeneobjekt som oppretter et `Behandling + Oppgave`-par. Dette er et bevisst valg for å gjenbruke eksisterende infrastruktur (tilstandsmaskiner, tilgangskontroll, saksbehandler-tildeling) uten å refaktorere `Oppgave` til å eksistere uten `Behandling`.

### Tilstandsmaskin

```
BEHANDLES ──→ FERDIGSTILT
```

Enklere enn `Oppgave`s 11-tilstands maskin. `BEHANDLES` er eneste aktive tilstand.

### Inngangsporter

Samme `OpprettGenerellOppgaveHendelse` brukes fra begge kildetyper:

```
Kafka (opprett_oppgave) ──→ OpprettOppgaveMottak ──┐
                                                    ├──→ GenerellOppgaveMediator.taImot()
REST POST /generell-oppgave  ─────────────────────┘
```

### Kafka-format

```json
{
  "@event_name": "opprett_oppgave",
  "ident": "12345678901",
  "tittel": "Sjekk adresseendring",
  "beskrivelse": "PDL registrerte ny adresse",
  "emneknagg": "AdresseEndring",
  "frist": "2026-05-01",
  "strukturertData": {
    "pdlHendelseId": "abc-123"
  }
}
```

> **Merk:** Kafka-feltet heter `emneknagg` (bakoverkompatibilitet), men mappes internt til `aarsak`.

### REST API

```
POST   /generell-oppgave              Opprett ny generell oppgave
GET    /generell-oppgave/{behandlingId}  Hent generell oppgave
PUT    /generell-oppgave/{behandlingId}/ferdigstill  Ferdigstill
```

## Ferdigstill-aksjonene

Saksbehandler velger utfall ved ferdigstilling:

| Aksjon | Beskrivelse |
|--------|-------------|
| `AVSLUTT` | Ferdigstill uten oppfølging |
| `OPPRETT_KLAGE` | Opprett klage på eksisterende sak |
| `OPPRETT_MANUELL_BEHANDLING` | Opprett ny manuell dagpengebehandling |
| `OPPRETT_REVURDERING_BEHANDLING` | Opprett revurdering |
| `OPPRETT_GENERELL_OPPGAVE` | Opprett ny generell oppgave som oppfølging |

### beholdOppgaven

Både ved opprettelse og ved `OPPRETT_GENERELL_OPPGAVE`-aksjon kan saksbehandler velge å få den nye oppgaven tildelt seg selv (`beholdOppgaven: true`).

## Frist-støtte

Dersom `frist` er satt, opprettes `Oppgave` i `PåVent`-tilstand med `utsattTil = frist`. Den eksisterende `OppgaveFristUtgåttJob` reaktiverer oppgaven automatisk når fristen passerer — ingen ny infrastruktur nødvendig.

## strukturertData

Fri JSON-blob for domene-spesifikk kontekst. Backend lagrer og eksponerer den uten å tolke innholdet. Frontend kan bruke den til å vise relevant kontekst for saksbehandler.

```json
"strukturertData": {
  "meldekortId": "MK-2026-01",
  "periodeId": "2026-W04"
}
```

## Relevant kode

| Fil | Beskrivelse |
|-----|-------------|
| `modell/.../generell/GenerellOppgave.kt` | Domeneobjekt med tilstandsmaskin |
| `modell/.../generell/GenerellOppgaveAksjon.kt` | 5 aksjontyper |
| `modell/.../hendelser/OpprettGenerellOppgaveHendelse.kt` | Felles hendelse for Kafka og REST |
| `mediator/.../generell/GenerellOppgaveMediator.kt` | Orkestrator |
| `mediator/.../generell/GenerellOppgaveApi.kt` | REST-endepunkter |
| `mediator/.../generell/GenerellOppgaveBehandler.kt` | Utfører aksjonene |
| `mediator/.../generell/OpprettOppgaveMottak.kt` | Kafka-konsument |
| `db/migration/V109__CREATE_GENERELL_OPPGAVE.sql` | Databaseskjema |

## Gjenstår / on hold

- **årsak-emneknagg for `settPåVent`**: `settPåVent()` legger ikke til årsak-emneknagg. Avventer avklaring med frontend om behovet.
- **`FerdigstillGenerellOppgaveRequest` på Innsending-endepunktet**: Utsatt til etter første versjon.
