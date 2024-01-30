package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.Regel

class Regelmotor(
    vararg regelsett: Regelsett,
) {
    private val muligeRegler: MutableList<Regel<*>> = regelsett.flatMap { it.regler }.toMutableList()
    private val plan: MutableList<Regel<*>> = mutableListOf()
    private val kjørteRegler: MutableList<Regel<*>> = mutableListOf()

    private lateinit var opplysninger: Opplysninger

    fun registrer(opplysninger: Opplysninger) {
        this.opplysninger = opplysninger
    }

    fun evaluer() {
        // TODO: Skriv om til EligibilityEngine fra DSL boka til Fowler

        aktiverRegler()
        while (plan.size > 0) {
            kjørRegelPlan()
            aktiverRegler()
        }
    }

    private fun kjørRegelPlan() {
        while (plan.size > 0) {
            kjør(plan.first())
        }
    }

    private fun kjør(regel: Regel<*>) {
        val opplysning = regel.blurp(opplysninger)
        kjørteRegler.add(regel)
        plan.remove(regel)
        opplysninger.leggTil(opplysning)
    }

    private fun aktiverRegler() {
        muligeRegler.filter {
            it.kanKjøre(opplysninger)
        }.forEach {
            plan.add(it)
        }
        plan.forEach {
            muligeRegler.remove(it)
        }
    }

    fun trenger(opplysningstype: Opplysningstype<*>): Set<Opplysningstype<*>> {
        return when (val regel = finn(opplysningstype)) {
            null -> return emptySet()
            else ->
                regel.avhengerAv.map {
                    if (finn(it) != null) {
                        trenger(it)
                    } else {
                        setOf(it)
                    }
                }.flatten().filterNot { opplysninger.har(it) }.toSet()
        }
    }

    private fun finn(opplysningstype: Opplysningstype<*>): Regel<*>? {
        return muligeRegler.find { it.produserer(opplysningstype) }
    }
}

class Regelsett(
    internal val regler: MutableList<Regel<*>> = mutableListOf(),
) {
    fun leggTil(regel: Regel<*>) {
        regler.add(regel)
    }
}
