package no.nav.dagpenger.saksbehandling.utsending.hendelser

import java.util.UUID

interface UtsendingHendelse {
    val behandlingId: UUID
}
