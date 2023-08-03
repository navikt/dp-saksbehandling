package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.ManuellSporing
import no.nav.dagpenger.behandling.NullSporing
import no.nav.dagpenger.behandling.QuizSporing
import no.nav.dagpenger.behandling.Sporing
import no.nav.dagpenger.behandling.api.models.BegrunnelseDTO
import no.nav.dagpenger.behandling.api.models.KildeDTO
import no.nav.dagpenger.behandling.api.models.QuizBegrunnelseDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlerDTO
import no.nav.dagpenger.behandling.api.models.SaksbehandlersBegrunnelseDTO
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.ZoneOffset

fun Sporing.toBegrunnelseDTO(): BegrunnelseDTO? {
    return when (this) {
        is QuizSporing -> QuizBegrunnelseDTO(
            kilde = KildeDTO.Quiz,
            utført = this.utført.atOffset(ZoneOffset.of("Europe/Oslo")),
            json = this.json,
        )

        is ManuellSporing -> SaksbehandlersBegrunnelseDTO(
            kilde = KildeDTO.Saksbehandler,
            utført = this.utført.atOffset(ZoneOffset.of("Europe/Oslo")),
            utførtAv = this.utførtAv.toSaksbehandlerDTO(),
            tekst = this.begrunnelse,
        )

        is NullSporing -> null
    }
}

private fun Saksbehandler.toSaksbehandlerDTO() = SaksbehandlerDTO(ident)
