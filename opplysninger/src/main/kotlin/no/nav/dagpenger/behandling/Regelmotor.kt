package no.nav.dagpenger.behandling

class Regelmotor(
    private val regler: MutableMap<Opplysningstype<*>, Regel<*>> = mutableMapOf(),
) {
    private lateinit var opplysninger: Opplysninger

    fun registrer(opplysninger: Opplysninger) {
        this.opplysninger = opplysninger
    }

    fun kjør(opplysning: Opplysning<*>) {
        val regelSomSkalKjøres = regler.filter { it.value.kanKjøre(opplysninger) }
        println(regelSomSkalKjøres)
        regelSomSkalKjøres.forEach {
            val verdi = it.value.blurp(opplysninger)
            opplysninger.leggTil(verdi)
        }
    }

    fun enAvRegel(
        produserer: Opplysningstype<Boolean>,
        vararg opplysningstype: Opplysningstype<Boolean>,
    ): Regel<Boolean> {
        return EnAvRegel(produserer, *opplysningstype).also { leggTil(produserer, it) }
    }

    fun multiplikasjon(
        produserer: Opplysningstype<Double>,
        vararg opplysningstype: Opplysningstype<Double>,
    ): Regel<Double> {
        return Multiplikasjon(produserer, *opplysningstype).also { leggTil(produserer, it) }
    }

    fun størreEnn(
        produserer: Opplysningstype<Boolean>,
        er: Opplysningstype<Double>,
        størreEnn: Opplysningstype<Double>,
    ): Regel<Boolean> {
        return StørreEnn(produserer, er, størreEnn).also { leggTil(produserer, it) }
    }

    private fun leggTil(
        produserer: Opplysningstype<*>,
        regel: Regel<*>,
    ) {
        if (regler.containsKey(produserer)) throw IllegalStateException("Regel for $produserer finnes allerede")
        produserer.utledesAv.addAll(regel.avhengerAv)
        regler[produserer] = regel
    }
}

abstract class Regel<T : Comparable<T>>(
    private val produserer: Opplysningstype<T>,
    val avhengerAv: List<Opplysningstype<*>> = emptyList(),
) {
    fun kanKjøre(opplysninger: List<Opplysning<*>>): Boolean =
        opplysninger.none { it.er(produserer) } &&
            avhengerAv.all { opplysninger.any { opplysning -> opplysning.er(it) } }

    protected abstract fun kjør(opplysninger: List<Opplysning<*>>): T

    fun blurp(opplysninger: List<Opplysning<*>>): Opplysning<T> {
        return Hypotese(produserer, kjør(opplysninger))
    }
}

class NullRegel<T : Comparable<T>> : Regel<T>(Opplysningstype("NullRegel")) {
    override fun kjør(opplysninger: List<Opplysning<*>>): T {
        throw IllegalStateException("NullRegel kan ikke kjøres")
    }
}

class EnAvRegel(
    produserer: Opplysningstype<Boolean>,
    private vararg val opplysningstyper: Opplysningstype<Boolean>,
) : Regel<Boolean>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: List<Opplysning<*>>): Boolean {
        return opplysningstyper.any { finn -> opplysninger.filter { it.er(finn) }.any { it.verdi as Boolean } }
    }
}

class Multiplikasjon(
    produserer: Opplysningstype<Double>,
    private vararg val opplysningstyper: Opplysningstype<Double>,
) : Regel<Double>(produserer, opplysningstyper.toList()) {
    override fun kjør(opplysninger: List<Opplysning<*>>): Double {
        val verdier =
            opplysningstyper.filter { opplysningstype ->
                opplysninger.any { it.er(opplysningstype) }
            }.map { opplysningstype ->
                opplysninger.find { it.er(opplysningstype) }?.verdi as Double
            }

        return verdier.reduce { acc, d -> acc * d }
    }

    override fun toString(): String {
        return "Multiplikasjon av ${opplysningstyper.joinToString(", ")}"
    }
}

class StørreEnn(
    produserer: Opplysningstype<Boolean>,
    private val a: Opplysningstype<Double>,
    private val b: Opplysningstype<Double>,
) : Regel<Boolean>(produserer, listOf(a, b)) {
    override fun kjør(opplysninger: List<Opplysning<*>>): Boolean {
        val verdi =
            opplysninger.filter { it.er(a) || it.er(b) }.let { opplysninger ->
                val a = opplysninger.find { it.er(a) }?.verdi as Double
                val b = opplysninger.find { it.er(b) }?.verdi as Double

                a > b
            }
        return verdi
    }
}
