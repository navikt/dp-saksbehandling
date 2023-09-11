package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.graph.DAGNode
import no.nav.dagpenger.behandling.graph.DAGNodeVisitor
import java.util.UUID

sealed class Steg<T> private constructor(
    val uuid: UUID = UUID.randomUUID(),
    val id: String,
    var svar: Svar<T>,
    var tilstand: Tilstand = Tilstand.IkkeUtført,
) {
    override fun equals(other: Any?): Boolean {
        return other != null && other is Steg<*> && this.uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    class Fastsettelse<T> private constructor(
        uuid: UUID = UUID.randomUUID(),
        id: String,
        svar: Svar<T>,
        tilstand: Tilstand,
    ) : Steg<T>(uuid, id = id, svar = svar, tilstand) {
        constructor(uuid: UUID = UUID.randomUUID(), id: String, svar: Svar<T>) : this(
            uuid,
            id,
            svar,
            Tilstand.IkkeUtført,
        )

        override val node: DAGNode<Steg<*>> = DAGNode(this)

        companion object {
            fun <T> rehydrer(uuid: UUID, id: String, svar: Svar<T>, tilstand: Tilstand): Steg<T> {
                return Fastsettelse(uuid, id, svar, tilstand)
            }
        }
    }

    class Vilkår private constructor(
        id: String,
        uuid: UUID = UUID.randomUUID(),
        svar: Svar<Boolean>,
        tilstand: Tilstand,
    ) : Steg<Boolean>(
        uuid = uuid,
        id = id,
        svar = svar,
        tilstand = tilstand,
    ) {
        constructor(id: String, uuid: UUID = UUID.randomUUID()) : this(
            id,
            uuid,
            Svar.BooleanSvar(null, NullSporing),
            Tilstand.IkkeUtført,
        )

        override val node: DAGNode<Steg<*>> = DAGNode(this)

        companion object {
            fun rehydrer(uuid: UUID, id: String, svar: Svar<Boolean>, tilstand: Tilstand): Steg<Boolean> {
                return Vilkår(id, uuid, svar, tilstand)
            }
        }
    }

    class Prosess private constructor(
        id: String,
        uuid: UUID = UUID.randomUUID(),
        svar: Svar<Boolean>,
        tilstand: Tilstand,
    ) : Steg<Boolean>(
        uuid = uuid,
        id = id,
        svar = svar,
        tilstand,
    ) {
        constructor(id: String, uuid: UUID = UUID.randomUUID()) : this(
            id,
            uuid,
            Svar.BooleanSvar(null, NullSporing),
            Tilstand.IkkeUtført,
        )

        override val node: DAGNode<Steg<*>> = DAGNode(this)

        companion object {
            fun rehydrer(uuid: UUID, id: String, svar: Svar<Boolean>, tilstand: Tilstand): Steg<Boolean> {
                return Prosess(id, uuid, svar, tilstand)
            }
        }
    }

    companion object {
        inline fun <reified B> fastsettelse(id: String) = Fastsettelse(id = id, svar = Svar.opprett<B>(NullSporing))
    }

    protected abstract val node: DAGNode<Steg<*>>

    fun avhengerAv(steg: Steg<*>): Steg<*> {
        node.addChild(steg.node)
        return steg
    }

    fun avhengigeSteg() = node.children().map { it.value }.toSet()

    fun nesteSteg(): Set<Steg<*>> {
        val criteria: (steg: Steg<*>) -> Boolean = { it.tilstand != Tilstand.Utført }
        val result = mutableSetOf<Steg<*>>()
        if (criteria(this)) {
            val x = node.getDescendants(false, criteria).map { it.value }
            if (x.isEmpty()) {
                result.add(this)
            } else {
                result.addAll(x)
            }
        }
        return result
    }

    fun alleSteg(): Set<Steg<*>> = setOf(this) + node.getDescendants().map { it.value }

    fun besvar(svar: T, sporing: Sporing) {
        require(sporing !is NullSporing) { "Sporing kan ikke være NullSporing" }
        this.svar = this.svar.besvar(svar, sporing)
        this.tilstand = Tilstand.Utført
        node.getAncestors().forEach {
            it.value.måGodkjennes()
        }
    }

    private fun måGodkjennes() {
        if (tilstand == Tilstand.Utført) {
            this.tilstand = Tilstand.MåGodkjennes
        }
    }

    fun accept(visitor: DAGNodeVisitor) {
        node.accept(visitor)
    }

    override fun toString(): String {
        return "Steg(type= ${this.javaClass.simpleName}, uuid=$uuid, id='$id')"
    }
}

enum class Rolle {
    Saksbehandler,
    Beslutter,
}
