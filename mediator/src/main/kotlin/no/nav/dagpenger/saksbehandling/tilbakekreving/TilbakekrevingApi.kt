package no.nav.dagpenger.saksbehandling.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.api.finnUUID
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingBehandlingStatusDTO
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingDTO
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingPeriodeDTO
import no.nav.dagpenger.saksbehandling.audit.Auditlogg
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser
import java.net.URI

internal fun Route.tilbakekrevingApi(
    tilbakekrevingMediator: TilbakekrevingMediator,
    applicationCallParser: ApplicationCallParser,
    auditlogg: Auditlogg,
) {
    route("tilbakekreving") {
        authenticate("azureAd") {
            route("{behandlingId}") {
                get {
                    val behandlingId = call.finnUUID("behandlingId")
                    val saksbehandler = applicationCallParser.saksbehandler(call)
                    val tilbakekrevingMedPersonIdent =
                        tilbakekrevingMediator.hent(
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                        )
                    auditlogg.les(
                        melding = "Så på tilbakekreving med id ${tilbakekrevingMedPersonIdent.tilbakekreving.behandlingId}",
                        ident = tilbakekrevingMedPersonIdent.personIdent,
                        saksbehandler = saksbehandler.navIdent,
                    )
                    call.respond(HttpStatusCode.OK, tilbakekrevingMedPersonIdent.tilbakekreving.tilTilbakekrevingDTO())
                }
            }
        }
    }
}

private fun Tilbakekreving.tilTilbakekrevingDTO(): TilbakekrevingDTO =
    TilbakekrevingDTO(
        tilbakekrevingBehandlingId = this.behandlingId,
        opprettet = this.opprettet,
        varselSendt = this.varselSendt,
        behandlingsstatus =
            TilbakekrevingBehandlingStatusDTO.fromValue(this.behandlingsstatus.name)
                ?: error("Ukjent behandlingsstatus: ${this.behandlingsstatus}"),
        totaltFeilutbetaltBelop = this.totaltFeilutbetaltBeløp,
        saksbehandlingURL = URI(this.saksbehandlingURL),
        fullstendigPeriode =
            TilbakekrevingPeriodeDTO(
                fom = this.fullstendigPeriode.fom,
                tom = this.fullstendigPeriode.tom,
            ),
    )
