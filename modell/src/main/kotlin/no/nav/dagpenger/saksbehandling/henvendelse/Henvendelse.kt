package no.nav.dagpenger.saksbehandling.henvendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettManuellBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class Henvendelse(
    val henvendelseId: UUID = UUIDv7.ny(),
    val person: Person,
    val journalpostId: String,
    val registrert: LocalDateTime,
    private var behandlerIdent: String? = null,
    private var tilstand: Tilstand = Tilstand.KlarTilBehandling,
    private val _tilstandslogg: HenvendelseTilstandslogg = HenvendelseTilstandslogg(),
) {
    val tilstandslogg: HenvendelseTilstandslogg
        get() = _tilstandslogg

    fun tildel(tildelHendelse: TildelHendelse) {
        tilstand.tildel(this, tildelHendelse)
    }

    fun leggTilbake(fjernAnsvarHendelse: FjernAnsvarHendelse) {
        tilstand.fjernAnsvar(this, fjernAnsvarHendelse)
    }

    fun ferdigstill(opprettManuellBehandlingHendelse: OpprettManuellBehandlingHendelse) {
        tilstand.ferdigstill(this, opprettManuellBehandlingHendelse)
    }

    fun tilstand(): Tilstand = tilstand

    fun behandlerIdent(): String? = behandlerIdent

    private fun endreTilstand(
        nyTilstand: Tilstand,
        hendelse: Hendelse,
    ) {
        logger.info {
            "Endrer tilstand fra ${this.tilstand.javaClass.simpleName} til ${nyTilstand.javaClass.simpleName} " +
                "for henvendelseId: ${this.henvendelseId} basert på hendelse: ${hendelse.javaClass.simpleName}"
        }
        this.tilstand = nyTilstand
        this._tilstandslogg.leggTil(nyTilstand, hendelse)
    }

    sealed interface Tilstand {
        fun tildel(
            henvendelse: Henvendelse,
            tildelHendelse: TildelHendelse,
        ) {
            ulovligTilstandsendring(henvendelse.henvendelseId) {
                "Kan ikke tildele henvendelse i tilstanden ${henvendelse.tilstand.javaClass.simpleName}"
            }
        }

        fun fjernAnsvar(
            henvendelse: Henvendelse,
            fjernAnsvarHendelse: FjernAnsvarHendelse,
        ) {
            ulovligTilstandsendring(henvendelse.henvendelseId) {
                "Kan ikke fjerne ansvar for henvendelse i tilstanden ${henvendelse.tilstand.javaClass.simpleName}"
            }
        }

        fun ferdigstill(
            henvendelse: Henvendelse,
            hendelse: OpprettManuellBehandlingHendelse,
        ) {
            ulovligTilstandsendring(henvendelse.henvendelseId) {
                "Kan ikke ferdigstille henvendelse i tilstanden ${henvendelse.tilstand.javaClass.simpleName}"
            }
        }

        object KlarTilBehandling : Tilstand {
            override fun tildel(
                henvendelse: Henvendelse,
                tildelHendelse: TildelHendelse,
            ) {
                henvendelse.endreTilstand(
                    nyTilstand = UnderBehandling,
                    hendelse = tildelHendelse,
                )
                henvendelse.behandlerIdent = tildelHendelse.ansvarligIdent
            }
        }

        data object UnderBehandling : Tilstand {
            override fun fjernAnsvar(
                henvendelse: Henvendelse,
                fjernAnsvarHendelse: FjernAnsvarHendelse,
            ) {
                henvendelse.endreTilstand(
                    nyTilstand = KlarTilBehandling,
                    hendelse = fjernAnsvarHendelse,
                )
                henvendelse.behandlerIdent = null
            }

            override fun ferdigstill(
                henvendelse: Henvendelse,
                hendelse: OpprettManuellBehandlingHendelse,
            ) {
                henvendelse.endreTilstand(
                    nyTilstand = Ferdigbehandlet,
                    hendelse = hendelse,
                )
            }
        }

        data object Ferdigbehandlet : Tilstand

        private fun ulovligTilstandsendring(
            henvendelseId: UUID,
            message: () -> String?,
        ): Nothing {
            withLoggingContext("henvendelseId" to henvendelseId.toString()) {
                logger.error(message)
            }
            // TODO gjøre noe med excaptions. Skal vi gjenbruke noe eller blir det igjen trippel opp med klasser
            // avhengig av om det er oppgave eller henvendelse eller kalge
            throw RuntimeException(message.invoke())
        }
    }
}
