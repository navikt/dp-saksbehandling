package no.nav.dagpenger.saksbehandling.tilbakekreving

import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import java.util.UUID

class TilbakekrevingMediator(
    private val oppgaveMediator: OppgaveMediator,
    private val personMediator: PersonMediator,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val transaksjoner: Transaksjoner,
) {
    fun håndter(tilbakekrevingHendelse: TilbakekrevingHendelse) {
        transaksjoner.transaksjon { ctx ->
            oppgaveMediator.håndter(
                tilbakekrevingHendelse = tilbakekrevingHendelse,
                ctx = ctx,
            )
            tilbakekrevingRepository.lagre(
                tilbakekreving = tilbakekrevingHendelse.tilbakekreving,
                ctx = ctx,
            )
        }
    }

    fun hent(
        behandlingId: UUID,
        saksbehandler: Saksbehandler,
    ): TilbakekrevingHendelse.Tilbakekreving {
        personMediator.harTilgang(
            behandlingId = behandlingId,
            saksbehandler = saksbehandler,
        )
        return tilbakekrevingRepository.hent(behandlingId)
    }
}
