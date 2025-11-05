package no.nav.dagpenger.saksbehandling.henvendelse

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

class Henvendelse private constructor(
    val henvendelseId: UUID = UUIDv7.ny(),
    val person: Person,
    val journalpostId: String,
    val mottatt: LocalDateTime,
    val skjemaKode: String,
    val kategori: Kategori,
    private var behandlerIdent: String? = null,
    private var tilstand: Tilstand = Tilstand.KlarTilBehandling,
    private val _tilstandslogg: HenvendelseTilstandslogg = HenvendelseTilstandslogg(),
) {
    companion object {
        fun opprett(
            hendelse: HenvendelseMottattHendelse,
            personProvider: (ident: String) -> Person,
        ): Henvendelse {
            return Henvendelse(
                person = personProvider.invoke(hendelse.ident),
                journalpostId = hendelse.journalpostId,
                mottatt = hendelse.registrertTidspunkt,
                skjemaKode = hendelse.skjemaKode,
                kategori = hendelse.kategori,
                tilstand = Tilstand.KlarTilBehandling,
            ).also {
                it._tilstandslogg.leggTil(Tilstand.Type.KLAR_TIL_BEHANDLING, hendelse)
            }
        }

        fun rehydrer(
            henvendelseId: UUID = UUIDv7.ny(),
            person: Person,
            journalpostId: String,
            mottatt: LocalDateTime,
            skjemaKode: String,
            kategori: Kategori,
            behandlerIdent: String?,
            tilstand: Tilstand,
            tilstandslogg: HenvendelseTilstandslogg,
        ): Henvendelse {
            return Henvendelse(
                henvendelseId = henvendelseId,
                person = person,
                journalpostId = journalpostId,
                mottatt = mottatt,
                skjemaKode = skjemaKode,
                kategori = kategori,
                behandlerIdent = behandlerIdent,
                tilstand = tilstand,
                _tilstandslogg = tilstandslogg,
            )
        }
    }

    val tilstandslogg: HenvendelseTilstandslogg
        get() = _tilstandslogg

    fun tildel(tildelHendelse: TildelHendelse) {
        tilstand.tildel(this, tildelHendelse)
    }

    fun leggTilbake(fjernAnsvarHendelse: FjernAnsvarHendelse) {
        tilstand.fjernAnsvar(this, fjernAnsvarHendelse)
    }

    fun ferdigstill(henvendelseFerdigstiltHendelse: HenvendelseFerdigstiltHendelse) {
        tilstand.ferdigstill(this, henvendelseFerdigstiltHendelse)
    }

    fun tilstand(): Tilstand = tilstand

    fun behandlerIdent(): String? = behandlerIdent

    override fun toString(): String {
        return "Henvendelse(henvendelseId=$henvendelseId, person=$person, journalpostId='$journalpostId', " +
            "mottatt=$mottatt, skjemaKode='$skjemaKode', kategori=$kategori, behandlerIdent=$behandlerIdent, " +
            "tilstand=${tilstand.type})"
    }

    private fun endreTilstand(
        nyTilstand: Tilstand,
        hendelse: Hendelse,
    ) {
        logger.info {
            "Endrer tilstand fra ${this.tilstand.javaClass.simpleName} til ${nyTilstand.javaClass.simpleName} " +
                "for henvendelseId: ${this.henvendelseId} basert på hendelse: ${hendelse.javaClass.simpleName}"
        }
        this.tilstand = nyTilstand
        this._tilstandslogg.leggTil(nyTilstand.type, hendelse)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Henvendelse

        if (henvendelseId != other.henvendelseId) return false
        if (person != other.person) return false
        if (journalpostId != other.journalpostId) return false
        if (mottatt != other.mottatt) return false
        if (skjemaKode != other.skjemaKode) return false
        if (kategori != other.kategori) return false
        if (behandlerIdent != other.behandlerIdent) return false
        if (tilstand != other.tilstand) return false
        if (_tilstandslogg != other._tilstandslogg) return false

        return true
    }

    override fun hashCode(): Int {
        var result = henvendelseId.hashCode()
        result = 31 * result + person.hashCode()
        result = 31 * result + journalpostId.hashCode()
        result = 31 * result + mottatt.hashCode()
        result = 31 * result + skjemaKode.hashCode()
        result = 31 * result + kategori.hashCode()
        result = 31 * result + (behandlerIdent?.hashCode() ?: 0)
        result = 31 * result + tilstand.hashCode()
        result = 31 * result + _tilstandslogg.hashCode()
        return result
    }

    fun harTilgang(utførtAv: Behandler) {
        when (utførtAv) {
            is Applikasjon -> {}
            is Saksbehandler -> {
                this.person.harTilgang(utførtAv)
            }
        }
    }

    sealed interface Tilstand {
        val type: Type

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
            hendelse: HenvendelseFerdigstiltHendelse,
        ) {
            ulovligTilstandsendring(henvendelse.henvendelseId) {
                "Kan ikke ferdigstille henvendelse i tilstanden ${henvendelse.tilstand.javaClass.simpleName}"
            }
        }

        enum class Type {
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIGBEHANDLET,
        }

        object KlarTilBehandling : Tilstand {
            override val type: Type = Type.KLAR_TIL_BEHANDLING

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
            override val type: Type = Type.UNDER_BEHANDLING

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
                hendelse: HenvendelseFerdigstiltHendelse,
            ) {
                henvendelse.endreTilstand(
                    nyTilstand = Ferdigbehandlet,
                    hendelse = hendelse,
                )
            }
        }

        data object Ferdigbehandlet : Tilstand {
            override val type: Type = Type.FERDIGBEHANDLET
        }

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
