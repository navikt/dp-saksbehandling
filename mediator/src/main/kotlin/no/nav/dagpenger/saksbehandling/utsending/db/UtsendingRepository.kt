package no.nav.dagpenger.saksbehandling.utsending.db

import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.util.UUID

interface UtsendingRepository {
    fun lagre(utsending: Utsending)

    fun utsendingFinnesForBehandling(behandlingId: UUID): Boolean

    fun slettUtsending(utsendingId: UUID): Int

    fun finnUtsendingForBehandlingId(behandlingId: UUID): Utsending?

    fun hentUtsendingForBehandlingId(behandlingId: UUID): Utsending
}

class UtsendingIkkeFunnet(message: String) : RuntimeException(message)
