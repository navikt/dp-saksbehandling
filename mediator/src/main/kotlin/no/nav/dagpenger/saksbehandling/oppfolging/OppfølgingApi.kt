package no.nav.dagpenger.saksbehandling.oppfolging

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.api.finnUUID
import no.nav.dagpenger.saksbehandling.api.models.BehandlingTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlingVariantDTO
import no.nav.dagpenger.saksbehandling.api.models.FerdigstillOppfolgingRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.OppfolgingDTO
import no.nav.dagpenger.saksbehandling.api.models.OpprettOppfolgingRequestDTO
import no.nav.dagpenger.saksbehandling.api.models.OpprettOppfolgingResponseDTO
import no.nav.dagpenger.saksbehandling.api.models.TynnBehandlingDTO
import no.nav.dagpenger.saksbehandling.api.models.TynnSakDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser
import no.nav.dagpenger.saksbehandling.jwt.jwt

internal fun Route.oppfølgingApi(
    oppfølgingMediator: OppfølgingMediator,
    applicationCallParser: ApplicationCallParser,
) {
    route("oppfolging") {
        authenticate("azureAd") {
            post {
                val saksbehandler = applicationCallParser.saksbehandler(call)
                val request = call.receive<OpprettOppfolgingRequestDTO>()

                val hendelse =
                    OpprettOppfølgingHendelse(
                        ident = request.personIdent,
                        aarsak = request.aarsak,
                        tittel = request.tittel,
                        beskrivelse = request.beskrivelse ?: "",
                        strukturertData = request.strukturertData ?: emptyMap(),
                        frist = request.frist,
                        beholdOppgaven = request.beholdOppgaven ?: false,
                        utførtAv = saksbehandler,
                    )

                val resultat = oppfølgingMediator.taImot(hendelse)

                call.respond(
                    HttpStatusCode.Created,
                    OpprettOppfolgingResponseDTO(
                        oppfølgingId = resultat.oppfølgingId,
                        oppgaveId = resultat.oppgaveId,
                    ),
                )
            }

            route("{behandlingId}") {
                get {
                    val behandlingId = call.finnUUID("behandlingId")
                    val saksbehandler = applicationCallParser.saksbehandler(call)

                    val oppfølging = oppfølgingMediator.hent(behandlingId, saksbehandler)
                    val lovligeSaker = oppfølgingMediator.hentLovligeSaker(oppfølging.person.ident)

                    call.respond(
                        HttpStatusCode.OK,
                        oppfølging.tilDTO(lovligeSaker),
                    )
                }

                route("ferdigstill") {
                    put {
                        val behandlingId = call.finnUUID("behandlingId")
                        val saksbehandler = applicationCallParser.saksbehandler(call)
                        val saksbehandlerToken = call.request.jwt()

                        val request = call.receive<FerdigstillOppfolgingRequestDTO>()

                        val aksjon =
                            when (request.behandlingsvariant) {
                                null -> OppfølgingAksjon.Avslutt(request.sakId)
                                BehandlingVariantDTO.RETT_TIL_DAGPENGER_MANUELL -> {
                                    val sakId = requireNotNull(request.sakId) { "sakId må være satt for manuell behandling" }
                                    OppfølgingAksjon.OpprettManuellBehandling(
                                        saksbehandlerToken = saksbehandlerToken,
                                        valgtSakId = sakId,
                                    )
                                }
                                BehandlingVariantDTO.RETT_TIL_DAGPENGER_REVURDERING -> {
                                    val sakId = requireNotNull(request.sakId) { "sakId må være satt for revurdering" }
                                    OppfølgingAksjon.OpprettRevurderingBehandling(
                                        saksbehandlerToken = saksbehandlerToken,
                                        valgtSakId = sakId,
                                    )
                                }
                                BehandlingVariantDTO.KLAGE -> {
                                    val sakId = requireNotNull(request.sakId) { "sakId må være satt for klage" }
                                    OppfølgingAksjon.OpprettKlage(sakId)
                                }
                                BehandlingVariantDTO.OPPFOLGING -> {
                                    val nyOppgave = requireNotNull(request.nyOppgave) { "nyOppgave må være satt for oppfølging" }
                                    OppfølgingAksjon.OpprettOppfølging(
                                        valgtSakId = request.sakId,
                                        tittel = nyOppgave.tittel,
                                        beskrivelse = nyOppgave.beskrivelse ?: "",
                                        aarsak = nyOppgave.aarsak,
                                        frist = nyOppgave.frist,
                                        beholdOppgaven = nyOppgave.beholdOppgaven ?: false,
                                    )
                                }
                            }

                        oppfølgingMediator.ferdigstill(
                            hendelse =
                                FerdigstillOppfølgingHendelse(
                                    oppfølgingId = behandlingId,
                                    aksjon = aksjon,
                                    vurdering = request.vurdering,
                                    utførtAv = saksbehandler,
                                ),
                        )

                        call.respond(HttpStatusCode.NoContent, "Oppfølging ferdigstilt")
                    }
                }
            }
        }
    }
}

private fun Oppfølging.tilDTO(lovligeSaker: List<Sak>): OppfolgingDTO =
    OppfolgingDTO(
        behandlingId = this.id,
        tittel = this.tittel,
        beskrivelse = this.beskrivelse,
        frist = this.frist,
        strukturertData = this.strukturertData,
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

private fun Oppfølging.toBehandling(): TynnBehandlingDTO? =
    when (val resultat = this.resultat()) {
        is Oppfølging.Resultat.Klage ->
            TynnBehandlingDTO(
                behandlingId = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.KLAGE,
            )

        is Oppfølging.Resultat.RettTilDagpenger ->
            TynnBehandlingDTO(
                behandlingId = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.RETT_TIL_DAGPENGER,
            )

        is Oppfølging.Resultat.Oppfølging ->
            TynnBehandlingDTO(
                behandlingId = resultat.behandlingId,
                behandlingType = BehandlingTypeDTO.OPPFØLGING,
            )

        else -> null
    }
