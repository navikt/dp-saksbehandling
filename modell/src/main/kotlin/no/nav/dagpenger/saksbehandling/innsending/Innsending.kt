package no.nav.dagpenger.saksbehandling.innsending

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetForSøknadHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.hendelser.TildelHendelse
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.Type.KLAR_TIL_BEHANDLING
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

class Innsending private constructor(
    val innsendingId: UUID = UUIDv7.ny(),
    val person: Person,
    val journalpostId: String,
    val mottatt: LocalDateTime,
    val skjemaKode: String,
    val kategori: Kategori,
    private var behandlerIdent: String? = null,
    private var tilstand: Tilstand = Tilstand.KlarTilBehandling,
    private val _tilstandslogg: InnsendingTilstandslogg = InnsendingTilstandslogg(),
) {
    companion object {
        fun opprett(
            hendelse: InnsendingMottattHendelse,
            personProvider: (ident: String) -> Person,
        ): Innsending {
            return Innsending(
                person = personProvider.invoke(hendelse.ident),
                journalpostId = hendelse.journalpostId,
                mottatt = hendelse.registrertTidspunkt,
                skjemaKode = hendelse.skjemaKode,
                kategori = hendelse.kategori,
                tilstand = Tilstand.KlarTilBehandling,
            ).also {
                it._tilstandslogg.leggTil(KLAR_TIL_BEHANDLING, hendelse)
            }
        }

        fun rehydrer(
            innsendingId: UUID = UUIDv7.ny(),
            person: Person,
            journalpostId: String,
            mottatt: LocalDateTime,
            skjemaKode: String,
            kategori: Kategori,
            behandlerIdent: String?,
            tilstand: Tilstand,
            tilstandslogg: InnsendingTilstandslogg,
        ): Innsending {
            return Innsending(
                innsendingId = innsendingId,
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

    val tilstandslogg: InnsendingTilstandslogg
        get() = _tilstandslogg

    fun tildel(tildelHendelse: TildelHendelse) {
        tilstand.tildel(this, tildelHendelse)
    }

    fun leggTilbake(fjernAnsvarHendelse: FjernAnsvarHendelse) {
        tilstand.fjernAnsvar(this, fjernAnsvarHendelse)
    }

    fun ferdigstill(innsendingFerdigstiltHendelse: InnsendingFerdigstiltHendelse) {
        tilstand.ferdigstill(this, innsendingFerdigstiltHendelse)
    }

    fun avbryt(behandlingOpprettetForSøknadHendelse: BehandlingOpprettetForSøknadHendelse) {
        tilstand.avbryt(this, behandlingOpprettetForSøknadHendelse)
    }

    fun gjelderSøknadMedId(søknadId: UUID): Boolean =
        kategori in setOf(Kategori.NY_SØKNAD, Kategori.GJENOPPTAK) && this.tilstandslogg.gjelderSøknadMedId(søknadId)

    private fun InnsendingTilstandslogg.gjelderSøknadMedId(søknadId: UUID): Boolean {
        return this.any {
            it.hendelse is InnsendingMottattHendelse && it.hendelse.søknadId == søknadId
        }
    }

    fun tilstand(): Tilstand = tilstand

    fun behandlerIdent(): String? = behandlerIdent

    override fun toString(): String {
        return "Innsending(innsendingId=$innsendingId, person=$person, journalpostId='$journalpostId', " +
            "mottatt=$mottatt, skjemaKode='$skjemaKode', kategori=$kategori, behandlerIdent=$behandlerIdent, " +
            "tilstand=${tilstand.type})"
    }

    private fun endreTilstand(
        nyTilstand: Tilstand,
        hendelse: Hendelse,
    ) {
        logger.info {
            "Endrer tilstand fra ${this.tilstand.javaClass.simpleName} til ${nyTilstand.javaClass.simpleName} " +
                "for innsendingId: ${this.innsendingId} basert på hendelse: ${hendelse.javaClass.simpleName}"
        }
        this.tilstand = nyTilstand
        this._tilstandslogg.leggTil(nyTilstand.type, hendelse)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Innsending

        if (innsendingId != other.innsendingId) return false
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
        var result = innsendingId.hashCode()
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
            innsending: Innsending,
            tildelHendelse: TildelHendelse,
        ) {
            ulovligTilstandsendring(innsending.innsendingId) {
                "Kan ikke tildele innsending i tilstanden ${innsending.tilstand.javaClass.simpleName}"
            }
        }

        fun fjernAnsvar(
            innsending: Innsending,
            fjernAnsvarHendelse: FjernAnsvarHendelse,
        ) {
            ulovligTilstandsendring(innsending.innsendingId) {
                "Kan ikke fjerne ansvar for innsending i tilstanden ${innsending.tilstand.javaClass.simpleName}"
            }
        }

        fun ferdigstill(
            innsending: Innsending,
            hendelse: InnsendingFerdigstiltHendelse,
        ) {
            ulovligTilstandsendring(innsending.innsendingId) {
                "Kan ikke ferdigstille innsending i tilstanden ${innsending.tilstand.javaClass.simpleName}"
            }
        }

        fun avbryt(
            innsending: Innsending,
            hendelse: BehandlingOpprettetForSøknadHendelse,
        ) {
            ulovligTilstandsendring(innsending.innsendingId) {
                "Kan ikke avbryte innsending i tilstanden ${innsending.tilstand.javaClass.simpleName}"
            }
        }

        enum class Type {
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIGBEHANDLET,
            AVBRUTT,
        }

        object KlarTilBehandling : Tilstand {
            override val type: Type = KLAR_TIL_BEHANDLING

            override fun tildel(
                innsending: Innsending,
                tildelHendelse: TildelHendelse,
            ) {
                innsending.endreTilstand(
                    nyTilstand = UnderBehandling,
                    hendelse = tildelHendelse,
                )
                innsending.behandlerIdent = tildelHendelse.ansvarligIdent
            }

            override fun avbryt(
                innsending: Innsending,
                behandlingOpprettetForSøknadHendelse: BehandlingOpprettetForSøknadHendelse,
            ) {
                innsending.endreTilstand(
                    nyTilstand = Avbrutt,
                    hendelse = behandlingOpprettetForSøknadHendelse,
                )
            }
        }

        data object UnderBehandling : Tilstand {
            override val type: Type = Type.UNDER_BEHANDLING

            override fun fjernAnsvar(
                innsending: Innsending,
                fjernAnsvarHendelse: FjernAnsvarHendelse,
            ) {
                innsending.endreTilstand(
                    nyTilstand = KlarTilBehandling,
                    hendelse = fjernAnsvarHendelse,
                )
                innsending.behandlerIdent = null
            }

            override fun ferdigstill(
                innsending: Innsending,
                innsendingFerdigstiltHendelse: InnsendingFerdigstiltHendelse,
            ) {
                innsending.endreTilstand(
                    nyTilstand = Ferdigbehandlet,
                    hendelse = innsendingFerdigstiltHendelse,
                )
            }
        }

        data object Ferdigbehandlet : Tilstand {
            override val type: Type = Type.FERDIGBEHANDLET
        }

        data object Avbrutt : Tilstand {
            override val type: Type = Type.AVBRUTT
        }

        private fun ulovligTilstandsendring(
            innsendingId: UUID,
            message: () -> String?,
        ): Nothing {
            withLoggingContext("innsendingId" to innsendingId.toString()) {
                logger.error(message)
            }
            // TODO gjøre noe med excaptions. Skal vi gjenbruke noe eller blir det igjen trippel opp med klasser
            // avhengig av om det er oppgave eller innsending eller kalge
            throw RuntimeException(message.invoke())
        }
    }
}
