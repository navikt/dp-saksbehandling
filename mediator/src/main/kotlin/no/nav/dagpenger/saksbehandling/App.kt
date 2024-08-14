package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main() {
    try {
        runBlocking { adressebeskyttelseMigrering() }
    } catch (e: Exception) {
        logger.error { "Feil ved migrering av adressebeskyttelse: ${e.message}" }
        exitProcess(-1)
    }
    ApplicationBuilder(Configuration.config).start()
}
