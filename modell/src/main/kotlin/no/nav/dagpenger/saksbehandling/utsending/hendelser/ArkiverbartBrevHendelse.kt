package no.nav.dagpenger.saksbehandling.utsending.hendelser

import de.slub.urn.URN
import java.util.UUID

data class ArkiverbartBrevHendelse(override val oppgaveId: UUID, val pdfUrn: URN) : UtsendingHendelse
