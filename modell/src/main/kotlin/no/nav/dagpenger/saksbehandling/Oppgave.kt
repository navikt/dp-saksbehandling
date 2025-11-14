package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.tilgangsstyring.SaksbehandlerErIkkeEier
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

sealed class Oppgave {
    abstract val oppgaveId: UUID
    abstract val opprettet: LocalDateTime
    abstract var behandlerIdent: String?
    protected abstract val emneknagger: MutableSet<String>
    protected abstract var utsattTil: LocalDate?
    protected abstract val tilstandslogg: OppgaveTilstandslogg
    abstract val person: Person
    abstract val behandling: Behandling
    protected abstract var meldingOmVedtak: MeldingOmVedtak
    protected abstract val tilstand: Tilstand

    protected fun requireEierskapTilOppgave(
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
    }
}
