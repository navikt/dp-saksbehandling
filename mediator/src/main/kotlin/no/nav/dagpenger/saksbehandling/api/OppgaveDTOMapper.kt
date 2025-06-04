package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.SikkerhetstiltakIntern
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlingTypeDTO
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
import no.nav.dagpenger.saksbehandling.api.models.TildeltOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import java.time.LocalDate
import java.util.UUID

internal class OppgaveDTOMapper(
    private val oppslag: Oppslag,
    private val oppgaveHistorikkDTOMapper: OppgaveHistorikkDTOMapper,
) {
    suspend fun lagPersonDTO(person: Person): PersonDTO {
        val pdlPerson = oppslag.hentPerson(person.ident)
        return lagPersonDTO(person, pdlPerson)
    }

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
                soknadId = oppgave.soknadId(),
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
        soknadId: UUID? = null,
    ): OppgaveDTO =

        OppgaveDTO(
            oppgaveId = oppgave.oppgaveId,
            behandlingId = oppgave.behandling.behandlingId,
            person = lagPersonDTO(oppgave.behandling.person, person),
            tidspunktOpprettet = oppgave.opprettet,
            behandlingType = oppgave.behandling.tilBehandlingTypeDTO(),
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
            soknadId = soknadId,
        )

    private fun mapGyldigeSikkerhetstiltak(person: PDLPersonIntern): List<SikkerhetstiltakDTO> {
        return person.sikkerhetstiltak.map { sikkerhetstiltakDto ->
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

    private fun lagPersonDTO(
        person: Person,
        pdlPersonIntern: PDLPersonIntern,
    ): PersonDTO {
        return PersonDTO(
            ident = person.ident,
            id = person.id,
            fornavn = pdlPersonIntern.fornavn,
            etternavn = pdlPersonIntern.etternavn,
            mellomnavn = pdlPersonIntern.mellomnavn,
            fodselsdato = pdlPersonIntern.fødselsdato,
            alder = pdlPersonIntern.alder,
            kjonn =
                when (pdlPersonIntern.kjønn) {
                    PDLPerson.Kjonn.MANN -> KjonnDTO.MANN
                    PDLPerson.Kjonn.KVINNE -> KjonnDTO.KVINNE
                    PDLPerson.Kjonn.UKJENT -> KjonnDTO.UKJENT
                },
            statsborgerskap = pdlPersonIntern.statsborgerskap,
            skjermesSomEgneAnsatte = person.skjermesSomEgneAnsatte,
            sikkerhetstiltak = mapGyldigeSikkerhetstiltak(pdlPersonIntern),
            adressebeskyttelseGradering =
                when (person.adressebeskyttelseGradering) {
                    AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG_UTLAND
                    AdressebeskyttelseGradering.STRENGT_FORTROLIG -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG
                    AdressebeskyttelseGradering.FORTROLIG -> AdressebeskyttelseGraderingDTO.FORTROLIG
                    AdressebeskyttelseGradering.UGRADERT -> AdressebeskyttelseGraderingDTO.UGRADERT
                },
        )
    }
}

internal fun Oppgave.tilOppgaveOversiktDTO() =
    OppgaveOversiktDTO(
        oppgaveId = this.oppgaveId,
        behandlingId = this.behandling.behandlingId,
        personIdent = this.behandling.person.ident,
        tidspunktOpprettet = this.opprettet,
        behandlingType = this.behandling.tilBehandlingTypeDTO(),
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

internal fun Oppgave.tilTildeltOppgaveDTO(): TildeltOppgaveDTO {
    return TildeltOppgaveDTO(
        nyTilstand = this.tilstand().tilOppgaveTilstandDTO(),
        behandlingType = this.behandling.tilBehandlingTypeDTO(),
    )
}

internal fun Behandling.tilBehandlingTypeDTO(): BehandlingTypeDTO {
    return when (this.type) {
        BehandlingType.RETT_TIL_DAGPENGER -> BehandlingTypeDTO.RETT_TIL_DAGPENGER
        BehandlingType.KLAGE -> BehandlingTypeDTO.KLAGE
        BehandlingType.MELDEKORT -> BehandlingTypeDTO.RETT_TIL_DAGPENGER
    }
}
