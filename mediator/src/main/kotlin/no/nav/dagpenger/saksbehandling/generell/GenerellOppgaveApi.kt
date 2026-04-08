package no.nav.dagpenger.saksbehandling.generell

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.api.finnUUID
import no.nav.dagpenger.saksbehandling.api.models.BehandlingTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlingVariantDTO
import no.nav.dagpenger.saksbehandling.api.models.FerdigstillGenerellOppgaveRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.GenerellOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.TynnBehandlingDTO
import no.nav.dagpenger.saksbehandling.api.models.TynnSakDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser
import no.nav.dagpenger.saksbehandling.jwt.jwt

internal fun Route.generellOppgaveApi(
    generellOppgaveMediator: GenerellOppgaveMediator,
    applicationCallParser: ApplicationCallParser,
) {
    route("generell-oppgave") {
        authenticate("azureAd") {
            route("{id}") {
                get {
                    val id = call.finnUUID("id")
                    val saksbehandler = applicationCallParser.saksbehandler(call)

                    val generellOppgave = generellOppgaveMediator.hent(id, saksbehandler)
                    val lovligeSaker = generellOppgaveMediator.hentLovligeSaker(generellOppgave.person.ident)

                    call.respond(
                        HttpStatusCode.OK,
                        generellOppgave.tilDTO(lovligeSaker),
                    )
                }

                route("ferdigstill") {
                    put {
                        val id = call.finnUUID("id")
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        val saksbehandlerToken = call.request.jwt()

                        val request = call.receive<FerdigstillGenerellOppgaveRequestDTO>()

                        val aksjon =
                            when (request.behandlingsvariant) {
                                null -> GenerellOppgaveAksjon.Avslutt(request.sakId)
                                BehandlingVariantDTO.RETT_TIL_DAGPENGER_MANUELL -> {
                                    val sakId = requireNotNull(request.sakId) { "sakId må være satt for manuell behandling" }
                                    GenerellOppgaveAksjon.OpprettManuellBehandling(
                                        saksbehandlerToken = saksbehandlerToken,
                                        valgtSakId = sakId,
                                    )
                                }
                                BehandlingVariantDTO.RETT_TIL_DAGPENGER_REVURDERING -> {
                                    val sakId = requireNotNull(request.sakId) { "sakId må være satt for revurdering" }
                                    GenerellOppgaveAksjon.OpprettRevurderingBehandling(
                                        saksbehandlerToken = saksbehandlerToken,
                                        valgtSakId = sakId,
                                    )
                                }
                                BehandlingVariantDTO.KLAGE -> {
                                    val sakId = requireNotNull(request.sakId) { "sakId må være satt for klage" }
                                    GenerellOppgaveAksjon.OpprettKlage(sakId)
                                }
                            }

                        generellOppgaveMediator.ferdigstill(
                            hendelse =
                                FerdigstillGenerellOppgaveHendelse(
                                    generellOppgaveId = id,
                                    aksjon = aksjon,
                                    vurdering = request.vurdering,
                                    utførtAv = saksbehandler,
                                ),
                        )

                        call.respond(HttpStatusCode.NoContent, "Generell oppgave ferdigstilt")
                    }
                }
            }
        }
    }
}

private fun GenerellOppgave.tilDTO(lovligeSaker: List<Sak>): GenerellOppgaveDTO =
    GenerellOppgaveDTO(
        tittel = this.tittel,
        beskrivelse = this.beskrivelse,
        lovligeSaker =
            lovligeSaker.map {
                TynnSakDTO(
                    sakId = it.sakId,
                    opprettetDato = it.opprettet,
                )
            },
        sakId = this.valgtSakId(),
        vurdering = this.vurdering(),
        nyBehandling = this.toBehandling(),
    )

private fun GenerellOppgave.toBehandling(): TynnBehandlingDTO? =
    when (val resultat = this.resultat()) {
        is GenerellOppgave.Resultat.Klage ->
            TynnBehandlingDTO(
                behandlingId = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.KLAGE,
            )

        is GenerellOppgave.Resultat.RettTilDagpenger ->
            TynnBehandlingDTO(
                behandlingId = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.RETT_TIL_DAGPENGER,
            )

        else -> null
    }
