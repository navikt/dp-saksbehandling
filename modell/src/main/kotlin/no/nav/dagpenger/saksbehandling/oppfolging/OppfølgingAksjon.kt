package no.nav.dagpenger.saksbehandling.oppfolging

import java.time.LocalDate
import java.util.UUID

sealed class OppfølgingAksjon {
    enum class Type {
        AVSLUTT,
        OPPRETT_MANUELL_BEHANDLING,
        OPPRETT_REVURDERING_BEHANDLING,
        OPPRETT_KLAGE,
        OPPRETT_OPPFOLGING,
    }

    abstract val valgtSakId: UUID?
    abstract val type: Type

    data class Avslutt(
        override val valgtSakId: UUID?,
    ) : OppfølgingAksjon() {
        override val type: Type = Type.AVSLUTT
    }

    data class OpprettManuellBehandling(
        val saksbehandlerToken: String,
        override val valgtSakId: UUID,
    ) : OppfølgingAksjon() {
        override val type: Type = Type.OPPRETT_MANUELL_BEHANDLING

        override fun toString() = "OpprettManuellBehandling(valgtSakId=$valgtSakId)"
    }

    data class OpprettRevurderingBehandling(
        val saksbehandlerToken: String,
        override val valgtSakId: UUID,
    ) : OppfølgingAksjon() {
        override val type: Type = Type.OPPRETT_REVURDERING_BEHANDLING

        override fun toString() = "OpprettRevurderingBehandling(valgtSakId=$valgtSakId)"
    }

    data class OpprettKlage(
        override val valgtSakId: UUID,
    ) : OppfølgingAksjon() {
        override val type: Type = Type.OPPRETT_KLAGE
    }

    data class OpprettOppfølging(
        override val valgtSakId: UUID?,
        val tittel: String,
        val beskrivelse: String = "",
        val aarsak: String,
        val frist: LocalDate? = null,
        val beholdOppgaven: Boolean = false,
    ) : OppfølgingAksjon() {
        override val type: Type = Type.OPPRETT_OPPFOLGING
    }
}
