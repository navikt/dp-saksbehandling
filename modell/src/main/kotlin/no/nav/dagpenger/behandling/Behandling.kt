package no.nav.dagpenger.behandling

import no.nav.dagpenger.aktivitetslogg.Aktivitetskontekst
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.behandling.BehandlingObserver.BehandlingEndretTilstand
import no.nav.dagpenger.behandling.BehandlingObserver.VedtakFattet
import no.nav.dagpenger.behandling.hendelser.Hendelse
import no.nav.dagpenger.behandling.hendelser.PersonHendelse
import no.nav.dagpenger.behandling.hendelser.VedtakStansetHendelse
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

interface Behandlingsstatus {
    fun utfall(): Utfall
    fun erFerdig(): Boolean
}

enum class Utfall {
    Innvilgelse,
    Avslag,
    Stans,
}

class Behandling private constructor(
    val person: Person,
    val steg: Set<Steg<*>>,
    val opprettet: LocalDateTime,
    val uuid: UUID,
    var tilstand: Tilstand,
    val behandler: List<PersonHendelse>,
    val sak: Sak,
) : Behandlingsstatus {
    private val observers = mutableSetOf<BehandlingObserver>()

    companion object {

        private fun tilstand(tilstand: String): Tilstand = when (tilstand) {
            "TilBehandling" -> TilBehandling
            "FerdigBehandlet" -> FerdigBehandlet
            else -> throw IllegalArgumentException("Ugyldig tilstand: $tilstand")
        }

        fun rehydrer(
            person: Person,
            steg: Set<Steg<*>>,
            opprettet: LocalDateTime,
            uuid: UUID,
            tilstand: String,
            behandler: List<PersonHendelse>,
            sak: Sak,
        ): Behandling = Behandling(
            person = person,
            steg = steg,
            opprettet = opprettet,
            uuid = uuid,
            tilstand = tilstand(tilstand),
            behandler = behandler,
            sak = sak,
        )
    }

    constructor(person: Person, hendelse: PersonHendelse, steg: Set<Steg<*>>, sak: Sak) : this(
        person,
        steg,
        LocalDateTime.now(),
        UUID.randomUUID(),
        TilBehandling,
        listOf(hendelse),
        sak,
    )

    fun accept(visitor: BehandlingVisitor) {
        visitor.visit(uuid, sak)
    }

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

    private fun fastsettelser(): Map<String, String> =
        alleSteg().filterIsInstance<Steg.Fastsettelse<*>>().associate { it.id to it.svar.toString() }

    fun utfør(kommando: UtførStegKommando) {
        tilstand.utfør(kommando, this)
    }

    override fun utfall(): Utfall {
        if (behandler.any { it is VedtakStansetHendelse }) return Utfall.Stans

        val alleVilkårOppfylt = steg.filterIsInstance<Steg.Vilkår>().all {
            it.svar.verdi == true
        }

        return if (alleVilkårOppfylt) Utfall.Innvilgelse else Utfall.Avslag
    }

    override fun erFerdig(): Boolean =
        steg.filterIsInstance<Steg.Vilkår>().any { it.svar.verdi == false } || steg.none { it.svar.ubesvart }

    fun besvar(uuid: UUID, verdi: String, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    fun besvar(uuid: UUID, verdi: Int, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    fun besvar(uuid: UUID, verdi: Double, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    fun besvar(uuid: UUID, verdi: LocalDate, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    fun besvar(uuid: UUID, verdi: Boolean, sporing: Sporing) = _besvar(uuid, verdi, sporing)

    private inline fun <reified T> _besvar(uuid: UUID, verdi: T, sporing: Sporing) {
        val stegSomSkalBesvares = alleSteg().single { it.uuid == uuid }

        val clazz = when (stegSomSkalBesvares.svar) {
            is Svar.BooleanSvar -> java.lang.Boolean::class.java
            is Svar.DoubleSvar -> java.lang.Double::class.java
            is Svar.IntegerSvar -> Integer::class.java
            is Svar.LocalDateSvar -> LocalDate::class.java
            is Svar.StringSvar -> java.lang.String::class.java
        }

        require(T::class.java == clazz) {
            "Fikk ${T::class.java}, forventet $clazz ved besvaring av steg med uuid: $uuid"
        }

        (stegSomSkalBesvares as Steg<T>).besvar(verdi, sporing)
    }

    fun addObserver(søknadObserver: BehandlingObserver) {
        observers.add(søknadObserver)
    }

    interface Tilstand : Aktivitetskontekst {
        fun entering(søknadHendelse: Hendelse, behandling: Behandling) {}

        override fun toSpesifikkKontekst(): SpesifikkKontekst = this.javaClass.canonicalName.split(".").last().let {
            SpesifikkKontekst(it, emptyMap())
        }

        fun utfør(kommando: UtførStegKommando, behandling: Behandling) {}
    }

    private object TilBehandling : Tilstand {

        override fun utfør(kommando: UtførStegKommando, behandling: Behandling) {
            kommando.besvar(behandling)

            if (behandling.erFerdig()) {
                behandling.varsleOmVedtak(kommando)
                behandling.endreTilstand(FerdigBehandlet, kommando)
            }
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

    private fun varsleOmVedtak(hendelse: Hendelse) {
        observers.forEach {
            it.vedtakFattet(
                VedtakFattet(
                    behandlingId = uuid,
                    ident = person.ident,
                    utfall = this.utfall(),
                    fastsettelser = this.fastsettelser(),
                    sakId = sak.id,
                ),
            )
        }
    }
}
