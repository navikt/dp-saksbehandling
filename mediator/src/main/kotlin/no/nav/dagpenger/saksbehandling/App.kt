package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

fun main() {
    runBlocking {
        this.launch(Dispatchers.IO) {
            logger.info { "Starter nye streams greier" }
            while (true) {
                delay(50000)
                logger.info { "Nye streams greier leverer" }
            }
        }
        this.launch {
            logger.info { "ApplicationBuilder starter" }
            ApplicationBuilder(Configuration.config).start()
        }
    }
}
