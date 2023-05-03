package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.BehandlingObserver.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.BehandlingObserver.VedtakFattet
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.InnstillingGodkjentHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface Behandlingsstatus {
    fun utfall(): Boolean
    fun erFerdig(): Boolean
}

interface Svarbart {
    fun besvar(uuid: UUID, verdi: String, sporing: Sporing)
    fun besvar(uuid: UUID, verdi: Int, sporing: Sporing)
    fun besvar(uuid: UUID, verdi: Double, sporing: Sporing)
    fun besvar(uuid: UUID, verdi: LocalDate, sporing: Sporing)
    fun besvar(uuid: UUID, verdi: Boolean, sporing: Sporing)
}

class Behandling private constructor(
    val person: Person,
    val steg: Set<Steg<*>>,
    val opprettet: LocalDateTime,
    val uuid: UUID,
    var tilstand: Tilstand,
    val behandler: List<Hendelse>,
) : Behandlingsstatus, Svarbart {
    private val observers = mutableSetOf<BehandlingObserver>()

    constructor(person: Person, hendelse: Hendelse, steg: Set<Steg<*>>) : this(
        person,
        steg,
        LocalDateTime.now(),
        UUID.randomUUID(),
        TilBehandling,
        listOf(hendelse),
    )

    fun nesteSteg(): Set<Steg<*>> {
        val map = steg.flatMap {
            it.nesteSteg()
        }
        return map.toSet()
    }

    fun alleSteg(): Set<Steg<*>> {
        return steg.flatMap {
            it.alleSteg()
        }.toSet()
    }

    fun erBehandlet() = tilstand == FerdigBehandlet

    fun fastsettelser(): Map<String, String> =
        alleSteg().filterIsInstance<Steg.Fastsettelse<*>>().associate { it.id to it.svar.toString() }

    fun håndter(hendelse: InnstillingGodkjentHendelse) {
        this.tilstand.håndter(hendelse, this)
    }

    override fun utfall(): Boolean = steg.filterIsInstance<Steg.Vilkår>().all {
        it.svar.verdi == true
    }

    override fun erFerdig(): Boolean =
        steg.filterIsInstance<Steg.Vilkår>().any { it.svar.verdi == false } || steg.none { it.svar.ubesvart }

    override fun besvar(uuid: UUID, verdi: String, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    override fun besvar(uuid: UUID, verdi: Int, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    override fun besvar(uuid: UUID, verdi: Double, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    override fun besvar(uuid: UUID, verdi: LocalDate, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    override fun besvar(uuid: UUID, verdi: Boolean, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    private inline fun <reified T> _besvar(uuid: UUID, verdi: T, sporing: Sporing) {
        val stegSomSkalBesvares = alleSteg().single { it.uuid == uuid }

        require(stegSomSkalBesvares.svar.clazz == T::class.java) {
            "Fikk ${T::class.java}, forventet ${stegSomSkalBesvares.svar.clazz} ved besvaring av steg med uuid: $uuid"
        }

        (stegSomSkalBesvares as Steg<T>).besvar(verdi, sporing)
    }

    fun addObserver(søknadObserver: BehandlingObserver) {
        observers.add(søknadObserver)
    }

    interface Tilstand : Aktivitetskontekst {
        fun entering(søknadHendelse: Hendelse, behandling: Behandling) {}

        fun håndter(hendelse: InnstillingGodkjentHendelse, behandling: Behandling) {
            throw IllegalStateException("Ikke gyldig i denne tilstanden")
        }

        override fun toSpesifikkKontekst(): SpesifikkKontekst = this.javaClass.canonicalName.split(".").last().let {
            SpesifikkKontekst(it, emptyMap())
        }
    }

    private object TilBehandling : Tilstand {
        override fun håndter(hendelse: InnstillingGodkjentHendelse, behandling: Behandling) {
            behandling.varsleOmVedtak(hendelse, behandling)
            behandling.endreTilstand(FerdigBehandlet, hendelse)
        }
    }

    private object FerdigBehandlet : Tilstand

    private fun endreTilstand(nyTilstand: Tilstand, hendelse: Hendelse) {
        if (nyTilstand == tilstand) {
            return // Vi er allerede i tilstanden
        }
        val forrigeTilstand = tilstand
        tilstand = nyTilstand
        hendelse.kontekst(tilstand)
        tilstand.entering(hendelse, this)
        varsleOmEndretTilstand(forrigeTilstand)
    }

    private fun varsleOmEndretTilstand(forrigeTilstand: Tilstand) {
        observers.forEach {
            it.behandlingEndretTilstand(
                BehandlingEndretTilstand(
                    behandlingId = uuid,
                    ident = person.ident,
                    gjeldendeTilstand = tilstand.javaClass.simpleName,
                    forrigeTilstand = forrigeTilstand.javaClass.simpleName,
                ),
            )
        }
    }

    private fun varsleOmVedtak(hendelse: InnstillingGodkjentHendelse, behandling: Behandling) {
        observers.forEach {
            it.vedtakFattet(
                VedtakFattet(
                    behandlingId = uuid,
                    ident = person.ident,
                    utfall = behandling.utfall(),
                    fastsettelser = behandling.fastsettelser(),
                ),
            )
        }
    }
}
