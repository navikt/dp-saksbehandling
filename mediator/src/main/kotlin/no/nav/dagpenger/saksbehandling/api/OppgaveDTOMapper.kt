package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.SikkerhetstiltakIntern
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.LovligeEndringerDTO
import no.nav.dagpenger.saksbehandling.api.models.NotatDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktResultatDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.api.models.SikkerhetstiltakDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import java.time.LocalDate

internal class OppgaveDTOMapper(
    private val oppslag: Oppslag,
    private val oppgaveHistorikkDTOMapper: OppgaveHistorikkDTOMapper,
) {
    suspend fun lagOppgaveDTO(oppgave: Oppgave): OppgaveDTO {
        return coroutineScope {
            val person = async { oppslag.hentPerson(oppgave.behandling.person.ident) }
            val journalpostIder = async { oppslag.hentJournalpostIder(oppgave) }
            val sisteSaksbehandlerDTO =
                oppgave.sisteSaksbehandler()?.let { saksbehandlerIdent ->
                    async { oppslag.hentBehandler(saksbehandlerIdent) }
                }
            val sisteBeslutterDTO =
                oppgave.sisteBeslutter()?.let { beslutterIdent ->
                    async { oppslag.hentBehandler(beslutterIdent) }
                }

            val oppgaveHistorikk =
                async {
                    oppgaveHistorikkDTOMapper.lagOppgaveHistorikk(oppgave.tilstandslogg)
                }
            lagOppgaveDTO(
                oppgave = oppgave,
                person = person.await(),
                journalpostIder = journalpostIder.await(),
                sisteSaksbehandlerDTO = sisteSaksbehandlerDTO?.await(),
                sisteBeslutterDTO = sisteBeslutterDTO?.await(),
                oppgaveHistorikk = oppgaveHistorikk.await(),
            )
        }
    }

    private fun lagOppgaveDTO(
        oppgave: Oppgave,
        person: PDLPersonIntern,
        journalpostIder: Set<String>,
        sisteSaksbehandlerDTO: BehandlerDTO? = null,
        sisteBeslutterDTO: BehandlerDTO? = null,
        oppgaveHistorikk: List<OppgaveHistorikkDTO> = emptyList(),
    ): OppgaveDTO =

        OppgaveDTO(
            oppgaveId = oppgave.oppgaveId,
            behandlingId = oppgave.behandling.behandlingId,
            person =
                PersonDTO(
                    ident = person.ident,
                    fornavn = person.fornavn,
                    etternavn = person.etternavn,
                    mellomnavn = person.mellomnavn,
                    fodselsdato = person.fødselsdato,
                    alder = person.alder,
                    kjonn =
                        when (person.kjønn) {
                            PDLPerson.Kjonn.MANN -> KjonnDTO.MANN
                            PDLPerson.Kjonn.KVINNE -> KjonnDTO.KVINNE
                            PDLPerson.Kjonn.UKJENT -> KjonnDTO.UKJENT
                        },
                    statsborgerskap = person.statsborgerskap,
                    skjermesSomEgneAnsatte = oppgave.behandling.person.skjermesSomEgneAnsatte,
                    sikkerhetstiltak = mapGyldigeSikkerhetstiltak(person),
                    adressebeskyttelseGradering =
                        when (oppgave.behandling.person.adressebeskyttelseGradering) {
                            AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG_UTLAND
                            AdressebeskyttelseGradering.STRENGT_FORTROLIG -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG
                            AdressebeskyttelseGradering.FORTROLIG -> AdressebeskyttelseGraderingDTO.FORTROLIG
                            AdressebeskyttelseGradering.UGRADERT -> AdressebeskyttelseGraderingDTO.UGRADERT
                        },
                ),
            tidspunktOpprettet = oppgave.opprettet,
            emneknagger = oppgave.emneknagger.toList(),
            tilstand = oppgave.tilstand().tilOppgaveTilstandDTO(),
            journalpostIder = journalpostIder.toList(),
            utsattTilDato = oppgave.utsattTil(),
            saksbehandler = sisteSaksbehandlerDTO,
            historikk = oppgaveHistorikk,
            beslutter = sisteBeslutterDTO,
            notat =
                oppgave.tilstand().notat()?.let {
                    NotatDTO(
                        tekst = it.hentTekst(),
                        sistEndretTidspunkt = it.sistEndretTidspunkt,
                    )
                },
            lovligeEndringer =
                LovligeEndringerDTO(
                    paaVentAarsaker =
                        when (oppgave.tilstand().type) {
                            UNDER_BEHANDLING -> UtsettOppgaveAarsakDTO.entries.map { it.value }
                            else -> emptyList()
                        },
                ),
        )

    private fun mapGyldigeSikkerhetstiltak(person: PDLPersonIntern) =
        person.sikkerhetstiltak.map { sikkerhetstiltakDto ->
            SikkerhetstiltakIntern(
                type = sikkerhetstiltakDto.type,
                beskrivelse = sikkerhetstiltakDto.beskrivelse,
                gyldigFom = sikkerhetstiltakDto.gyldigFom,
                gyldigTom = sikkerhetstiltakDto.gyldigTom,
            )
        }.filter { sikkerhetstiltakIntern -> sikkerhetstiltakIntern.erGyldig(LocalDate.now()) }.map {
            SikkerhetstiltakDTO(beskrivelse = it.beskrivelse, gyldigTom = it.gyldigTom)
        }
}

internal fun Oppgave.tilOppgaveOversiktDTO() =
    OppgaveOversiktDTO(
        oppgaveId = this.oppgaveId,
        behandlingId = this.behandling.behandlingId,
        personIdent = this.behandling.person.ident,
        tidspunktOpprettet = this.opprettet,
        emneknagger = this.emneknagger.toList(),
        skjermesSomEgneAnsatte = this.behandling.person.skjermesSomEgneAnsatte,
        adressebeskyttelseGradering =
            when (this.behandling.person.adressebeskyttelseGradering) {
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG_UTLAND
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG
                AdressebeskyttelseGradering.FORTROLIG -> AdressebeskyttelseGraderingDTO.FORTROLIG
                AdressebeskyttelseGradering.UGRADERT -> AdressebeskyttelseGraderingDTO.UGRADERT
            },
        tilstand = this.tilstand().tilOppgaveTilstandDTO(),
        saksbehandlerIdent = this.behandlerIdent,
        behandlerIdent = this.behandlerIdent,
        utsattTilDato = this.utsattTil(),
    )

internal fun List<Oppgave>.tilOppgaveOversiktDTOListe(): List<OppgaveOversiktDTO> {
    return this.map { oppgave -> oppgave.tilOppgaveOversiktDTO() }
}

internal fun PostgresOppgaveRepository.OppgaveSøkResultat.tilOppgaverOversiktResultatDTO(): OppgaveOversiktResultatDTO {
    return OppgaveOversiktResultatDTO(
        oppgaver = this.oppgaver.tilOppgaveOversiktDTOListe(),
        totaltAntallOppgaver = this.totaltAntallOppgaver,
    )
}

internal fun Oppgave.Tilstand.tilOppgaveTilstandDTO(): OppgaveTilstandDTO {
    return when (this) {
        is Oppgave.Opprettet -> throw InternDataException("Ikke tillatt å eksponere oppgavetilstand Opprettet")
        is Oppgave.KlarTilBehandling -> OppgaveTilstandDTO.KLAR_TIL_BEHANDLING
        is Oppgave.UnderBehandling -> OppgaveTilstandDTO.UNDER_BEHANDLING
        is Oppgave.FerdigBehandlet -> OppgaveTilstandDTO.FERDIG_BEHANDLET
        is Oppgave.PåVent -> OppgaveTilstandDTO.PAA_VENT
        is Oppgave.KlarTilKontroll -> OppgaveTilstandDTO.KLAR_TIL_KONTROLL
        is Oppgave.UnderKontroll -> OppgaveTilstandDTO.UNDER_KONTROLL
        is Oppgave.AvventerLåsAvBehandling -> OppgaveTilstandDTO.AVVENTER_LÅS_AV_BEHANDLING
        is Oppgave.AvventerOpplåsingAvBehandling -> OppgaveTilstandDTO.AVVENTER_OPPLÅSING_AV_BEHANDLING
        is Oppgave.BehandlesIArena -> OppgaveTilstandDTO.BEHANDLES_I_ARENA
        else -> throw InternDataException("Ukjent tilstand: $this")
    }
}
