package no.nav.dagpenger.saksbehandling.tilbakekreving

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.api.finnUUID
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingBehandlingStatusDTO
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingDTO
import no.nav.dagpenger.saksbehandling.api.models.TilbakekrevingPeriodeDTO
import no.nav.dagpenger.saksbehandling.audit.Auditlogg
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import no.nav.dagpenger.saksbehandling.jwt.ApplicationCallParser
import java.net.URI

internal fun Route.tilbakekrevingApi(
    oppgaveMediator: OppgaveMediator,
    applicationCallParser: ApplicationCallParser,
    auditlogg: Auditlogg,
) {
    route("tilbakekreving") {
        authenticate("azureAd") {
            route("{behandlingId}") {
                get {
                    val behandlingId = call.finnUUID("behandlingId")
                    val saksbehandler = applicationCallParser.saksbehandler(call)
                    val oppgave =
                        oppgaveMediator.hentOppgaveForBehandling(
                            behandlingId = behandlingId,
                            saksbehandler = saksbehandler,
                        )
                    auditlogg.les("Så en tilbakekreving", oppgave.person.ident, saksbehandler.navIdent)

                    val tilbakekrevingDTO =
                        oppgave
                            .takeIf { it.behandling.utløstAv == HendelseBehandler.Intern.Tilbakekreving }
                            ?.tilstandslogg
                            ?.firstNotNullOfOrNull { it.hendelse as? TilbakekrevingHendelse }
                            ?.tilTilbakekrevingDTO()

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
        opprettet = tilbakekreving.opprettet,
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
