import Tils.KlarForKontroll
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

abstract class Opp(
    open val oppgaveId: UUID,
    open val opprettet: LocalDateTime,
    open var tilstand: Tils,
    open var behandlerIdent: String? = null,
) {
    private val tilstandslogg: Logg = Logg()

    internal fun endreTilstand(
        nyTilstand: Tils,
        hendelse: Hendelse,
    ) {
        println("Endrer fra tilstand ${this.tilstand.type} til ${nyTilstand.type} for oppgave $oppgaveId")
        this.tilstand = nyTilstand
        this.tilstandslogg.leggTil(nyTilstand = nyTilstand.type, hendelse = hendelse)
    }
}

data class KlageOppgave(
    override val oppgaveId: UUID,
    override val opprettet: LocalDateTime,
    override var tilstand: Tils,
    override var behandlerIdent: String? = null,
) : Opp(oppgaveId, opprettet, tilstand, behandlerIdent) {
    fun tildel(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse) {
        tilstand.tildel(oppgave = this, settOppgaveAnsvarHendelse = settOppgaveAnsvarHendelse)
    }

    fun leggTilbake(fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse) {
        tilstand.leggTilbake(oppgave = this, fjernOppgaveAnsvarHendelse = fjernOppgaveAnsvarHendelse)
    }
}

data class RettTilDPOppgave(
    override val oppgaveId: UUID,
    override val opprettet: LocalDateTime,
    override var tilstand: Tils,
    override var behandlerIdent: String? = null,
) : Opp(oppgaveId, opprettet, tilstand, behandlerIdent) {
    fun tildel(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse) {
        tilstand.tildel(oppgave = this, settOppgaveAnsvarHendelse = settOppgaveAnsvarHendelse)
    }

    fun leggTilbake(fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse) {
        tilstand.leggTilbake(oppgave = this, fjernOppgaveAnsvarHendelse = fjernOppgaveAnsvarHendelse)
    }

    fun sendTilKontroll(sendTilKontrollHendelse: SendTilKontrollHendelse) {
        tilstand.sendTilKontroll(oppgave = this, sendTilKontrollHendelse = sendTilKontrollHendelse)
    }
}

sealed interface Tils {
    val type: Typ

    enum class Typ {
        KLAR,
        BEHANDLE_RETT_TIL_DAGPENGER,
        BEHANDLE_KLAGE,
        BEHANDLE_INNSENDING,
        TIL_KONTROLL,
        KONTROLLERES,
        FERDIG,
    }

    fun tildel(
        oppgave: Opp,
        settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
    ) {
        throw IllegalArgumentException("Kan ikke sende oppgave til kontroll i tilstand $type")
    }

    fun leggTilbake(
        oppgave: Opp,
        fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
    ) {
        throw IllegalArgumentException("Kan ikke legge tilbake oppgave i tilstand $type")
    }

    fun sendTilKontroll(
        oppgave: Opp,
        sendTilKontrollHendelse: SendTilKontrollHendelse,
    ) {
        throw IllegalArgumentException("Kan ikke sende oppgave til kontroll i tilstand $type")
    }

    object Klar : Tils {
        override val type: Typ = Typ.KLAR

        override fun tildel(
            oppgave: Opp,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
            var nyTilstand: Tils = BehandleRettTilDagpenger
            when (oppgave) {
                is RettTilDPOppgave -> nyTilstand = BehandleRettTilDagpenger
                is KlageOppgave -> nyTilstand = BehandleKlage
            }
            oppgave.endreTilstand(nyTilstand = nyTilstand, hendelse = settOppgaveAnsvarHendelse)
        }
    }

    object BehandleRettTilDagpenger : Tils {
        override val type: Typ = Typ.BEHANDLE_RETT_TIL_DAGPENGER

        override fun leggTilbake(
            oppgave: Opp,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.behandlerIdent = null
            oppgave.endreTilstand(nyTilstand = Klar, hendelse = fjernOppgaveAnsvarHendelse)
        }

        override fun sendTilKontroll(
            oppgave: Opp,
            sendTilKontrollHendelse: SendTilKontrollHendelse,
        ) {
            oppgave.endreTilstand(nyTilstand = KlarForKontroll, hendelse = sendTilKontrollHendelse)
        }
    }

    object BehandleKlage : Tils {
        override val type: Typ = Typ.BEHANDLE_KLAGE

        override fun leggTilbake(
            oppgave: Opp,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.behandlerIdent = null
            oppgave.endreTilstand(nyTilstand = Klar, hendelse = fjernOppgaveAnsvarHendelse)
        }
    }

    object KlarForKontroll : Tils {
        override val type: Typ = Typ.TIL_KONTROLL

        override fun tildel(
            oppgave: Opp,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
            oppgave.endreTilstand(nyTilstand = Kontrolleres, hendelse = settOppgaveAnsvarHendelse)
        }
    }

    object Kontrolleres : Tils {
        override val type: Typ = Typ.KONTROLLERES

        override fun leggTilbake(
            oppgave: Opp,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.behandlerIdent = null
            oppgave.endreTilstand(nyTilstand = KlarForKontroll, hendelse = fjernOppgaveAnsvarHendelse)
        }
    }

    object Ferdig : Tils {
        override val type: Typ = Typ.FERDIG
    }
}

data class Logg(
    private val tilstandsendringer: MutableList<TilstEndr> = mutableListOf(),
) : List<TilstEndr> by tilstandsendringer {
    companion object {
        fun rehydrer(tilstandsendringer: List<TilstEndr>): Logg = Logg(tilstandsendringer.toMutableList())
    }

    init {
        tilstandsendringer.sorterEtterTidspunkt()
    }

    fun leggTil(
        nyTilstand: Tils.Typ,
        hendelse: Hendelse,
    ) {
        tilstandsendringer.add(0, TilstEndr(tilstand = nyTilstand, hendelse = hendelse))
    }

    private fun MutableList<TilstEndr>.sorterEtterTidspunkt(): Unit = this.sortByDescending { it.tidspunkt }
}

data class TilstEndr(
    val id: UUID = UUIDv7.ny(),
    val tilstand: Tils.Typ,
    val hendelse: Hendelse,
    val tidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
)

class OppgaveTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<Tils.Typ>> = listOf(),
) : Tilstandslogg<Tils.Typ>(tilstandsendringer.toMutableList()) {
    constructor(vararg tilstandsEndringer: Tilstandsendring<Tils.Typ>) : this(tilstandsEndringer.toMutableList())
}

fun main() {
    val mikke =
        Saksbehandler(
            navIdent = "mikke",
            grupper = emptySet(),
            tilganger = setOf(SAKSBEHANDLER),
        )
    val pluto =
        Saksbehandler(
            navIdent = "pluto",
            grupper = emptySet(),
            tilganger = setOf(SAKSBEHANDLER, BESLUTTER),
        )
    val klage =
        KlageOppgave(
            oppgaveId = UUIDv7.ny(),
            opprettet = LocalDateTime.now(),
            tilstand = Tils.Klar,
        )
    klage.tildel(
        SettOppgaveAnsvarHendelse(
            oppgaveId = klage.oppgaveId,
            ansvarligIdent = mikke.navIdent,
            utførtAv = mikke,
        ),
    )
    klage.leggTilbake(
        FjernOppgaveAnsvarHendelse(
            oppgaveId = klage.oppgaveId,
            utførtAv = mikke,
        ),
    )
    val søknadOppgave =
        RettTilDPOppgave(
            oppgaveId = UUIDv7.ny(),
            opprettet = LocalDateTime.now(),
            tilstand = Tils.Klar,
        )
    søknadOppgave.tildel(
        SettOppgaveAnsvarHendelse(
            oppgaveId = søknadOppgave.oppgaveId,
            ansvarligIdent = mikke.navIdent,
            utførtAv = mikke,
        ),
    )
    søknadOppgave.sendTilKontroll(
        SendTilKontrollHendelse(
            oppgaveId = søknadOppgave.oppgaveId,
            utførtAv = mikke,
        ),
    )
    søknadOppgave.tildel(
        SettOppgaveAnsvarHendelse(
            oppgaveId = søknadOppgave.oppgaveId,
            ansvarligIdent = pluto.navIdent,
            utførtAv = pluto,
        ),
    )
    søknadOppgave.leggTilbake(
        FjernOppgaveAnsvarHendelse(
            oppgaveId = søknadOppgave.oppgaveId,
            utførtAv = pluto,
        ),
    )
    søknadOppgave.tildel(
        SettOppgaveAnsvarHendelse(
            oppgaveId = søknadOppgave.oppgaveId,
            ansvarligIdent = pluto.navIdent,
            utførtAv = pluto,
        ),
    )
}
