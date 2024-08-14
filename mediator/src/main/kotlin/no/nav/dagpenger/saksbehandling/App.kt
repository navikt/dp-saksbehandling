package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main() {
    try {
        runBlocking { adressebeskyttelseMigrering() }
    } catch (e: Exception) {
        exitProcess(-1)
    }
    ApplicationBuilder(Configuration.config).start()
}
