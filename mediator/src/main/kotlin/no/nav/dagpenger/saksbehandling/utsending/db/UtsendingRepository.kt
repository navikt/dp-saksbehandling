package no.nav.dagpenger.saksbehandling.utsending.db

import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.util.UUID

interface UtsendingRepository {
    fun lagre(utsending: Utsending)

    fun hent(oppgaveId: UUID): Utsending

    fun finnUtsendingFor(oppgaveId: UUID): Utsending?

    fun hentUtsendingFor(behandlingId: UUID): Utsending
}

class UtsendingIkkeFunnet(message: String) : RuntimeException(message)
