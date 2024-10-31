package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.NotatDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkBeslutterDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag

internal class OppgaveDTOMapper(
    private val pdlKlient: PDLKlient,
    private val journalpostIdClient: JournalpostIdClient,
    private val saksbehandlerOppslag: SaksbehandlerOppslag,
    private val repository: OppgaveRepository,
) {
    suspend fun lagOppgaveDTO(oppgave: Oppgave): OppgaveDTO {
        return coroutineScope {
            val person = async { pdlKlient.person(oppgave.behandling.person.ident).getOrThrow() }
            val journalpostIder = async { journalpostIdClient.hentJournalPostIder(oppgave.behandling) }
            val sisteSaksbehandlerDTO =
                oppgave.sisteSaksbehandler()?.let { saksbehandlerIdent ->
                    async { saksbehandlerOppslag.hentSaksbehandler(saksbehandlerIdent) }
                }
            val sisteBeslutterDTO =
                oppgave.sisteBeslutter()?.let { beslutterIdent ->
                    async { saksbehandlerOppslag.hentSaksbehandler(beslutterIdent) }
                }

            lagOppgaveDTO(
                oppgave = oppgave,
                person = person.await(),
                journalpostIder = journalpostIder.await(),
                sisteSaksbehandlerDTO = sisteSaksbehandlerDTO?.await(),
                sisteBeslutterDTO = sisteBeslutterDTO?.await(),
                oppgaveHistorikk = lagOppgaveHistorikk(oppgave.tilstandslogg),
            )
        }
    }

    private fun lagOppgaveHistorikk(tilstandslogg: Tilstandslogg): List<OppgaveHistorikkDTO> {
        return tilstandslogg.filter {
            it.tilstand != Oppgave.Tilstand.Type.UNDER_KONTROLL
        }.map {
            Triple(it.id, it.hendelse.utførtAv, repository.finnNotat(it.id))
        }.filter { it.third != null }.map {
            OppgaveHistorikkDTO(
                type = OppgaveHistorikkDTO.Type.notat,
                tidspunkt = it.third!!.sistEndretTidspunkt,
                beslutter =
                    OppgaveHistorikkBeslutterDTO(
                        navn = (it.second as Saksbehandler).navIdent,
                        rolle = OppgaveHistorikkBeslutterDTO.Rolle.beslutter,
                    ),
                tittel = "Notat",
                body = it.third!!.hentTekst(),
            )
        }
    }

    private suspend fun JournalpostIdClient.hentJournalPostIder(behandling: Behandling): Set<String> {
        return when (val hendelse = behandling.hendelse) {
            is SøknadsbehandlingOpprettetHendelse -> {
                this.hentJournalpostId(hendelse.søknadId).map {
                    setOf(it)
                }.getOrElse {
                    emptySet()
                }
            }

            else -> emptySet()
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
        )
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

internal fun List<Oppgave>.tilOppgaverOversiktDTO(): List<OppgaveOversiktDTO> {
    return this.map { oppgave -> oppgave.tilOppgaveOversiktDTO() }
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
        else -> throw InternDataException("Ukjent tilstand: $this")
    }
}
