package no.nav.dagpenger.saksbehandling.henvendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

class Henvendelse(
    val henvendelseId: UUID = UUIDv7.ny(),
    val person: Person,
    val journalpostId: String,
    val registrert: LocalDateTime,
    private var behandlerIdent: String? = null,
    private var tilstand: Tilstand = Tilstand.KlarTilBehandling,
    private val tilstandslogg: HenvendelseTilstandslogg = HenvendelseTilstandslogg(),
) {
    fun tildel(tildelHendelse: TildelHendelse) {
        tilstand.tildel(this, tildelHendelse)
    }

    fun tilstand(): Tilstand = tilstand

    fun behandlerIdent(): String? = behandlerIdent

    sealed interface Tilstand {
        fun tildel(
            henvendelse: Henvendelse,
            tildelHendelse: TildelHendelse,
        ) {
            ulovligTilstandsendring(henvendelse.henvendelseId) {
                "Kan ikke tildele henvendelse i tilstanden ${henvendelse.tilstand.javaClass.simpleName}"
            }
        }

        object KlarTilBehandling : Tilstand {
            override fun tildel(
                henvendelse: Henvendelse,
                tildelHendelse: TildelHendelse,
            ) {
                henvendelse.behandlerIdent = tildelHendelse.ansvarligIdent
                henvendelse.tilstand = UnderBehandling
            }
        }

        data object UnderBehandling : Tilstand

        data class Ferdigbehandlet(val behandlerIdent: String, val ferdigstiltTidspunkt: LocalDateTime) : Tilstand

        private fun ulovligTilstandsendring(
            henvendelseId: UUID,
            message: () -> String?,
        ): Nothing {
            withLoggingContext("henndelseId" to henvendelseId.toString()) {
                logger.error(message)
            }
            // todo gjøre noe med excaptions  skal vi gjenbruke noe eller blir det igjen trippel opp med klasser
            // avhengig av om det er oppgave eller henvendelse eller kalge
            throw RuntimeException(message.invoke())
        }
    }

    class HenvendelseTilstandslogg()
}
