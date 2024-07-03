package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

fun main() {
    ApplicationBuilder(Configuration.config).start()
}
