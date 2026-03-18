package no.nav.dagpenger.saksbehandling.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.finnUUID
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingBehandlingStatusDTO
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingDTO
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingPeriodeDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import java.net.URI

internal fun Route.tilbakekrevingApi(oppgaveRepository: OppgaveRepository) {
    route("tilbakekreving") {
        authenticate("azureAd") {
            route("{behandlingId}") {
                get {
                    val behandlingId = call.finnUUID("behandlingId")

                    val tilbakekrevingDTO =
                        oppgaveRepository
                            .finnOppgaveFor(behandlingId)
                            ?.takeIf {
                                it.behandling.utløstAv == UtløstAvType.TILBAKEKREVING
                            }?.tilstandslogg
                            ?.firstOrNull {
                                it.hendelse is TilbakekrevingHendelse
                            }?.let {
                                (it.hendelse as TilbakekrevingHendelse).tilTilbakekrevingDTO()
                            }

                    when (tilbakekrevingDTO) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respond(HttpStatusCode.OK, tilbakekrevingDTO)
                    }
                }
            }
        }
    }
}

private fun TilbakekrevingHendelse.tilTilbakekrevingDTO() =
    TilbakekrevingDTO(
        tilbakekrevingBehandlingId = tilbakekreving.behandlingId,
        sakOpprettet = tilbakekreving.sakOpprettet,
        varselSendt = tilbakekreving.varselSendt,
        behandlingsstatus =
            TilbakekrevingBehandlingStatusDTO.fromValue(tilbakekreving.behandlingsstatus.name)
                ?: error("Ukjent behandlingsstatus: ${tilbakekreving.behandlingsstatus}"),
        totaltFeilutbetaltBelop = tilbakekreving.totaltFeilutbetaltBeløp,
        saksbehandlingURL = URI(tilbakekreving.saksbehandlingURL),
        fullstendigPeriode =
            TilbakekrevingPeriodeDTO(
                fom = tilbakekreving.fullstendigPeriode.fom,
                tom = tilbakekreving.fullstendigPeriode.tom,
            ),
    )
