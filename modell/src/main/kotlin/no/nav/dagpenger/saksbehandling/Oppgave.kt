package no.nav.dagpenger.saksbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.hendelser.AnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.tilgangsstyring.SaksbehandlerErIkkeEier
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

sealed class Oppgave<T : Oppgave.Tilstand> {
    abstract val oppgaveId: UUID
    abstract val opprettet: LocalDateTime
    abstract var behandlerIdent: String?
    protected abstract val emneknagger: MutableSet<String>
    abstract var utsattTil: LocalDate?
    protected abstract val tilstandslogg: OppgaveTilstandslogg
    abstract val person: Person
    abstract val behandling: Behandling
    protected abstract var meldingOmVedtak: MeldingOmVedtak
    protected abstract var tilstand: T

    fun requireEierskapTilOppgave(
        saksbehandler: Saksbehandler,
        hendelseNavn: String,
    ) {
        require(this.erEierAvOppgave(saksbehandler)) {
            throw SaksbehandlerErIkkeEier(
                "Ulovlig hendelse $hendelseNavn på oppgave i tilstand $${tilstandType()} uten å eie oppgaven. " +
                    "Oppgave eies av ${this.behandlerIdent} og ikke ${saksbehandler.navIdent}",
            )
        }
    }

    fun kontrollertBrev() = this.meldingOmVedtak.kontrollertGosysBrev

    fun erEierAvOppgave(saksbehandler: Saksbehandler): Boolean = this.behandlerIdent == saksbehandler.navIdent

    fun emneknagger(): Set<String> = emneknagger.toSet()

    fun tilstandslogg(): OppgaveTilstandslogg = tilstandslogg

    fun personIdent() = person.ident

    fun tilstand(): Tilstand = tilstand

    fun meldingOmVedtakKilde() = this.meldingOmVedtak.kilde

    fun egneAnsatteTilgangskontroll(saksbehandler: Saksbehandler) = this.person.egneAnsatteTilgangskontroll(saksbehandler)

    fun adressebeskyttelseTilgangskontroll(saksbehandler: Saksbehandler) = this.person.adressebeskyttelseTilgangskontroll(saksbehandler)

    fun utsattTil() = this.utsattTil

    fun tilstandType() = this.tilstand.type

    fun tildel(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse) {
        egneAnsatteTilgangskontroll(settOppgaveAnsvarHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(settOppgaveAnsvarHendelse.utførtAv)
        tilstand.tildel(this, settOppgaveAnsvarHendelse)
    }

    fun endreTilstand(
        nyTilstand: Tilstand,
        hendelse: Hendelse,
    ) {
        tilstandslogg.leggTil(nyTilstand.type, hendelse)
        @Suppress("UNCHECKED_CAST")
        tilstand = nyTilstand as T
    }

    enum class KontrollertBrev {
        JA,
        NEI,
        IKKE_RELEVANT,
    }

    data class MeldingOmVedtak(
        var kilde: MeldingOmVedtakKilde,
        var kontrollertGosysBrev: KontrollertBrev,
    )

    enum class MeldingOmVedtakKilde {
        DP_SAK,
        GOSYS,
        INGEN,
    }

    interface Tilstand {
        fun notat(): Notat? = null

        fun tildel(
            oppgave: Oppgave<*>,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å tildele oppgave i tilstand $type",
            )
        }

        val type: Type

        enum class Type {
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            PAA_VENT,
            KLAR_TIL_KONTROLL,
            UNDER_KONTROLL,
            FERDIG_BEHANDLET,
            AVBRUTT,
            OPPRETTET,
            AVVENTER_LÅS_AV_BEHANDLING,
            AVVENTER_OPPLÅSING_AV_BEHANDLING,
            ;

            companion object {
                val values
                    get() = entries.toSet()

                // Tilstander som ikke lenger er i bruk, skal ikke kunne søkes på
                val søkbareTilstander =
                    entries
                        .toSet()
                        .minus(OPPRETTET)
                        .minus(AVVENTER_LÅS_AV_BEHANDLING)
                        .minus(AVVENTER_OPPLÅSING_AV_BEHANDLING)
            }
        }

        fun ulovligTilstandsendring(
            oppgaveId: UUID,
            message: String,
        ): Nothing {
            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                logger.error { message }
            }
            throw UlovligTilstandsendringException(message)
        }

        class UlovligTilstandsendringException(
            message: String,
        ) : RuntimeException(message)
    }

    fun sisteSaksbehandler(): String? =
        runCatching {
            tilstandslogg().firstOrNull { it.tilstand == UNDER_BEHANDLING && it.hendelse is AnsvarHendelse }?.let {
                (it.hendelse as AnsvarHendelse).ansvarligIdent
            }
        }.onFailure { e -> logger.error(e) { "Feil ved henting av siste saksbehandler for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()

    fun sisteBeslutter(): String? =
        runCatching {
            tilstandslogg().firstOrNull { it.tilstand == UNDER_KONTROLL && it.hendelse is AnsvarHendelse }?.let {
                (it.hendelse as AnsvarHendelse).ansvarligIdent
            }
        }.onFailure { e -> logger.error(e) { "Feil ved henting av siste beslutter for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()

    fun soknadId(): UUID? =
        runCatching {
            tilstandslogg().firstOrNull { it.hendelse is ForslagTilVedtakHendelse }?.let {
                val hendelse = it.hendelse as ForslagTilVedtakHendelse
                when (hendelse.behandletHendelseType) {
                    "Søknad" -> UUID.fromString(hendelse.behandletHendelseId)
                    "Manuell", "Meldekort" -> {
                        logger.info {
                            "behandletHendelseType is ${hendelse.behandletHendelseType} " +
                                "for oppgave: ${this.oppgaveId} " +
                                "søknadId eksisterer derfor ikke"
                        }
                        null
                    }

                    else -> {
                        logger.error { "Ukjent behandletHendelseType ${hendelse.behandletHendelseType} for oppgave ${this.oppgaveId}" }
                        null
                    }
                }
            }
        }.onFailure { e ->
            logger.error(e) { "Feil ved henting av ForslagTilVedtakHendelse og dermed søknadId for oppgave:  ${this.oppgaveId}" }
        }.getOrThrow()
}
