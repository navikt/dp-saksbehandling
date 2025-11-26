package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import java.time.LocalDateTime
import java.util.UUID

data class InnsendingMottattHendelse(
    val ident: String,
    val journalpostId: String,
    val registrertTidspunkt: LocalDateTime,
    val søknadId: UUID?,
    val skjemaKode: String,
    val kategori: Kategori,
    override val utførtAv: Behandler = Applikasjon("dp-mottak"),
) : Hendelse(utførtAv) {
    fun erEttersendingMedSøknadId() = kategori == Kategori.ETTERSENDING && søknadId != null
}

enum class Kategori {
    NY_SØKNAD,
    GJENOPPTAK,
    GENERELL,
    UTDANNING,
    ETABLERING,
    KLAGE,
    ANKE,
    KLAGE_FORSKUDD,
    ETTERSENDING,
    UKJENT_SKJEMA_KODE,
    UTEN_BRUKER,
}
