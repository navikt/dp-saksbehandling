package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.util.UUID

class OpprettholdtVedtakHendelse(
    val behandlingId: UUID,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
