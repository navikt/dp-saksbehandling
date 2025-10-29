package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.toUUID
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

internal object TestHelper {
    val personIdent = "41952264877"
    val personId = UUID.fromString("019a2f67-aea7-7b29-84d5-56db0b1dc48a")
    val testPerson = lagPerson(ident = personIdent, id = personId)

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

    fun lagPerson(
        ident: String = personIdent,
        id: UUID = personId,
        addresseBeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
        skjermesSomEgneAnsatte: Boolean = false,
    ) = Person(
        id = id,
        ident = ident,
        skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
        adressebeskyttelseGradering = addresseBeskyttelseGradering,
    )

    internal object TestSaksbehandler {
        val navIdent = "SaksbehandlerIdent"
        val defaultSaksbehandlerADGruppe = listOf("SaksbehandlerADGruppe")
    }

    internal object TestBeslutter {
        val navIdent = "BeslutterIdent"
    }

    val oppgaveId = UUIDv7.ny()
    val søknadId = "01953789-f215-744e-9f6e-a55509bae78b".toUUID()

    fun lagTilstandLogg(): Tilstandslogg {
        return Tilstandslogg.rehydrer(
            listOf(
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
                            ansvarligIdent = TestSaksbehandler.navIdent,
                            utførtAv = Saksbehandler(TestSaksbehandler.navIdent, emptySet()),
                        ),
                    tidspunkt = opprettetNå.minusDays(2),
                ),
                Tilstandsendring(
                    tilstand = UNDER_KONTROLL,
                    hendelse =
                        SettOppgaveAnsvarHendelse(
                            oppgaveId = oppgaveId,
                            ansvarligIdent = TestBeslutter.navIdent,
                            utførtAv = Saksbehandler(TestBeslutter.navIdent, emptySet()),
                        ),
                    tidspunkt = opprettetNå.minusDays(1),
                ),
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
        tilstandslogg: Tilstandslogg = Tilstandslogg(),
        oppgaveId: UUID = TestHelper.oppgaveId,
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

    val behandlingId = UUIDv7.ny()

    fun lagBehandling(
        behandlingId: UUID = TestHelper.behandlingId,
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

    private val opprettetNå = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)

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
}
