package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import java.util.UUID

data class UtsendingDistribuert(
    val behandlingId: UUID,
    val utsendingId: UUID,
    val ident: String,
    val journalpostId: String,
    val distribusjonId: String,
    override val utførtAv: Behandler = Applikasjon("dp-saksbehandling"),
) : Hendelse(utførtAv)
