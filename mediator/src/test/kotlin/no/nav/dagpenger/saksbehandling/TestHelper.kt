package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.innsending.Innsending
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

internal object TestHelper {
    val personIdent = "41952264877"
    val personId = UUID.fromString("019a2f67-aea7-7b29-84d5-56db0b1dc48a")
    val behandlingId = UUIDv7.ny()
    val opprettetNå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)
    val oppgaveId = UUIDv7.ny()
    val saksbehandler =
        Saksbehandler(
            "SaksbehandlerIdent",
            setOf(Configuration.saksbehandlerADGruppe),
            setOf(
                SAKSBEHANDLER,
            ),
        )

    val testPerson =
        Person(
            id = personId,
            ident = personIdent,
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        )

    val testBehandling =
        lagBehandling(
            behandlingId = behandlingId,
            opprettet = opprettetNå,
            hendelse = TomHendelse,
            utløstAvType = UtløstAvType.SØKNAD,
        )

    val testOppgave =
        lagOppgave(
            oppgaveId = oppgaveId,
            opprettet = opprettetNå,
            person = testPerson,
            behandling = testBehandling,
        )

    val pdlPerson =
        PDLPersonIntern(
            ident = personIdent,
            fornavn = "PETTER",
            etternavn = "SMART",
            mellomnavn = null,
            fødselsdato = of(2000, 1, 1),
            alder = 0,
            statsborgerskap = "NOR",
            kjønn = PDLPerson.Kjonn.UKJENT,
            adresseBeskyttelseGradering = UGRADERT,
            sikkerhetstiltak =
                listOf(
                    SikkerhetstiltakIntern(
                        type = "Tiltakstype",
                        beskrivelse = "To ansatte i samtale",
                        gyldigFom = LocalDate.now(),
                        gyldigTom = LocalDate.now().plusDays(1),
                    ),
                ),
        )

    val testInnsending = lagInnsending()

    fun lagInnsending(
        innsendingId: UUID = UUIDv7.ny(),
        person: Person = testPerson,
        journalpostId: String = "journalpostId",
        mottatt: LocalDateTime = opprettetNå,
        skjemaKode: String = "skjemaKode",
        kategori: Kategori = Kategori.GENERELL,
    ): Innsending {
        return Innsending.rehydrer(
            innsendingId = innsendingId,
            person = person,
            journalpostId = journalpostId,
            mottatt = mottatt,
            skjemaKode = skjemaKode,
            kategori = kategori,
        )
    }

    fun lagPerson(
        ident: String = randomFnr(),
        id: UUID = UUIDv7.ny(),
        addresseBeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
        skjermesSomEgneAnsatte: Boolean = false,
    ) = Person(
        id = id,
        ident = ident,
        skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
        adressebeskyttelseGradering = addresseBeskyttelseGradering,
    )

    val beslutter =
        Saksbehandler(
            "BeslutterIdent",
            setOf(Configuration.saksbehandlerADGruppe, Configuration.beslutterADGruppe),
            setOf(BESLUTTER, SAKSBEHANDLER),
        )

    val søknadId = "01953789-f215-744e-9f6e-a55509bae78b".toUUID()

    fun lagTilstandLogg(): OppgaveTilstandslogg {
        return OppgaveTilstandslogg(
            Tilstandsendring(
                tilstand = KLAR_TIL_BEHANDLING,
                hendelse =
                    ForslagTilVedtakHendelse(
                        ident = personIdent,
                        behandletHendelseId = søknadId.toString(),
                        behandletHendelseType = "Søknad",
                        behandlingId = UUID.randomUUID(),
                    ),
                tidspunkt = opprettetNå,
            ),
            Tilstandsendring(
                tilstand = UNDER_BEHANDLING,
                hendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                tidspunkt = opprettetNå.minusDays(2),
            ),
            Tilstandsendring(
                tilstand = UNDER_KONTROLL,
                hendelse =
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgaveId,
                        ansvarligIdent = beslutter.navIdent,
                        utførtAv = beslutter,
                    ),
                tidspunkt = opprettetNå.minusDays(1),
            ),
        )
    }

    fun lagOppgave(
        tilstand: Oppgave.Tilstand = KlarTilBehandling,
        opprettet: LocalDateTime = opprettetNå,
        saksbehandlerIdent: String? = null,
        person: Person = testPerson,
        behandling: Behandling = lagBehandling(opprettet = opprettet),
        emneknagger: Set<String> = emptySet(),
        utsattTil: LocalDate? = null,
        tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
        oppgaveId: UUID = UUIDv7.ny(),
    ): Oppgave {
        return Oppgave.rehydrer(
            oppgaveId = oppgaveId,
            behandlerIdent = saksbehandlerIdent,
            opprettet = opprettet,
            emneknagger = emneknagger,
            tilstand = tilstand,
            utsattTil = utsattTil,
            tilstandslogg = tilstandslogg,
            person = person,
            behandling = behandling,
            meldingOmVedtak =
                Oppgave.MeldingOmVedtak(
                    kilde = DP_SAK,
                    kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                ),
        )
    }

    fun lagBehandling(
        behandlingId: UUID = UUIDv7.ny(),
        opprettet: LocalDateTime = opprettetNå,
        hendelse: Hendelse = TomHendelse,
        utløstAvType: UtløstAvType = UtløstAvType.SØKNAD,
    ): Behandling {
        return Behandling(
            behandlingId = behandlingId,
            opprettet = opprettet,
            hendelse = hendelse,
            utløstAv = utløstAvType,
        )
    }

    fun lagUtsending(
        tilstand: Utsending.Tilstand,
        behandlingId: UUID,
    ) = Utsending(
        tilstand = tilstand,
        ident = personIdent,
        behandlingId = behandlingId,
        brev = "brev",
        pdfUrn = null,
        journalpostId = "journalpostId",
        distribusjonId = "distribusjonId",
        utsendingSak = null,
    )

    private fun randomFnr(): String {
        val birthDate =
            of(
                Random.nextInt(1940, 2020),
                Random.nextInt(1, 13),
                Random.nextInt(1, 28),
            )
        val datePart = birthDate.format(DateTimeFormatter.ofPattern("ddMMyy"))
        val individnummer = (Random.nextInt(100, 999)).toString()
        val kontrollsifre = (Random.nextInt(10, 99)).toString()
        return datePart + individnummer + kontrollsifre
    }
}
