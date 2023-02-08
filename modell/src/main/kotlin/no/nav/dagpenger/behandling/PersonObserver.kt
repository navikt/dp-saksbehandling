package no.nav.dagpenger.behandling

interface PersonObserver {
    fun vedtakFattet(vedtakFattet: VedtakFattet)

    data class VedtakFattet(val ident: String, val utfall: Boolean)
}
