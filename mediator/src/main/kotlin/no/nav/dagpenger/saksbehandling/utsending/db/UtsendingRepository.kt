package no.nav.dagpenger.saksbehandling.utsending.db

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst.IkkeAktiv
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.util.UUID

interface UtsendingRepository {
    fun lagre(
        utsending: Utsending,
        ctx: Transaksjonskontekst = IkkeAktiv,
    )

    fun utsendingFinnesForBehandling(behandlingId: UUID): Boolean

    fun finnUtsendingForBehandlingId(behandlingId: UUID): Utsending?

    fun hentUtsendingForBehandlingId(behandlingId: UUID): Utsending

    fun slettUtsending(utsendingId: UUID): Int

    fun slettUtsending(
        utsendingId: UUID,
        kontekst: Transaksjonskontekst.Aktiv,
    ): Int
}

class UtsendingIkkeFunnet(
    message: String,
) : RuntimeException(message)
