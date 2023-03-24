package no.nav.dagpenger.behandling

class Behandling(
    val steg: Set<Steg> = emptySet(),
) {
    fun nesteSteg(): Set<Steg> {
        return steg.flatMap {
            it.nesteSteg()
        }.toSet()
    }
}

fun behandling(block: StegBuilder.() -> Unit): Behandling {
    val builder = StegBuilder()
    builder.block()
    return Behandling(builder.getSteg())
}

class Svar<T>(val verdi: T) {
    companion object {
        val Ubesvart = Svar(Unit)
    }
}
