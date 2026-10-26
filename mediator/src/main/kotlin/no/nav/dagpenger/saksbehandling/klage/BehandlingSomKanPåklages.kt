package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.HendelseBehandler
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingSomKanPåklages(
    val behandlingId: UUID,
    val hendelseBehandler: HendelseBehandler,
    val ferdigbehandletTidspunkt: LocalDateTime,
)
