package no.nav.dagpenger.saksbehandling.utsending.db

import no.nav.dagpenger.saksbehandling.utsending.Utsending
import java.time.LocalDateTime
import java.util.UUID

interface UtsendingRepository {
    fun lagre(utsending: Utsending)

    fun hent(oppgaveId: UUID): Utsending

    fun finnUtsendingFor(oppgaveId: UUID): Utsending?

    fun utsendingFinnesForOppgave(oppgaveId: UUID): Boolean

    fun utsendingFinnesForBehandling(behandlingId: UUID): Boolean

    fun slettUtsending(utsendingID: UUID): Int

    fun hentVentendeUtsendinger(intervallAntallTimer: Int = 24): List<Pair<Utsending, LocalDateTime>>
}

class UtsendingIkkeFunnet(message: String) : RuntimeException(message)

class IdentIkkeFunnet(message: String) : RuntimeException(message)
