package no.nav.dagpenger.behandling

import java.time.LocalDate

/*class Svar2<T>(val verdi: T?, val clazz: Class<T>, val sporing: Sporing) {
    fun besvar(verdi: T, sporing: Sporing) = Svar(verdi, this.clazz, sporing)
    fun nullstill() = Svar(null, this.clazz, NullSporing)
    val ubesvart get() = verdi == null

    override fun toString() = verdi.toString()
}*/

sealed class Svar<T>() {
    abstract val sporing: Sporing
    abstract val verdi: T?

    abstract fun besvar(verdi: T, sporing: Sporing): Svar<T>

    // Define subtypes for different value types
    data class BooleanSvar(override val verdi: Boolean?, override val sporing: Sporing) : Svar<Boolean>() {
        override fun besvar(verdi: Boolean, sporing: Sporing) = BooleanSvar(verdi, sporing)
    }

    data class StringSvar(override val verdi: String?, override val sporing: Sporing) : Svar<String>() {
        override fun besvar(verdi: String, sporing: Sporing) = StringSvar(verdi, sporing)
    }

    data class IntegerSvar(override val verdi: Int?, override val sporing: Sporing) : Svar<Int>() {
        override fun besvar(verdi: Int, sporing: Sporing) = IntegerSvar(verdi, sporing)
    }

    data class DoubleSvar(override val verdi: Double?, override val sporing: Sporing) : Svar<Double>() {
        override fun besvar(verdi: Double, sporing: Sporing) = DoubleSvar(verdi, sporing)
    }

    data class LocalDateSvar(override val verdi: LocalDate?, override val sporing: Sporing) : Svar<LocalDate>() {
        override fun besvar(verdi: LocalDate, sporing: Sporing) = LocalDateSvar(verdi, sporing)
    }

    // Use the `value` property for type-specific access
    val ubesvart get() = verdi == null

    override fun toString() = verdi?.toString() ?: "null"

    companion object {
        // Factory methods for creating Svar instances
        inline fun <reified T> opprett(sporing: Sporing): Svar<T> {
            return when (T::class.java.simpleName) {
                "Boolean" -> BooleanSvar(null, sporing) as Svar<T>
                "String" -> StringSvar(null, sporing) as Svar<T>
                "Integer" -> IntegerSvar(null, sporing) as Svar<T>
                "Double" -> DoubleSvar(null, sporing) as Svar<T>
                "LocalDate" -> LocalDateSvar(null, sporing) as Svar<T>
                else -> throw IllegalArgumentException("Ugyldig svartype: ${T::class.java.simpleName}")
            }
        }
    }
}
