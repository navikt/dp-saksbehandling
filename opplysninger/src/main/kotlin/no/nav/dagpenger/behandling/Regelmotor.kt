package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.Regel
import java.time.LocalDateTime

class Regelmotor(
    vararg regelsett: Regelsett,
) {
    private val muligeRegler: MutableList<Regel<*>> = regelsett.flatMap { it.regler }.toMutableList()
    private val plan: MutableList<Regel<*>> = mutableListOf()
    private val kjørteRegler: MutableList<Regel<*>> = mutableListOf()
    private lateinit var opplysninger: Opplysninger

    init {
        require(muligeRegler.groupBy { it.produserer }.all { it.value.size == 1 }) {
            "Regelsett inneholder flere regler som produserer samme opplysningstype."
        }
    }

    fun registrer(opplysninger: Opplysninger) {
        this.opplysninger = opplysninger
    }

    fun evaluer() {
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
        val opplysning = regel.lagProdukt(opplysninger)
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

    // @todo: Lage graf representasjon av denne? Som kan traverseres, spørres om hvilke noder som er tilgjengelige fra en node, etc.
    fun trenger(
        opplysningstype: Opplysningstype<*>,
        fraDato: LocalDateTime,
    ): Set<Opplysningstype<*>> {
        return when (val regel = finnRegel(opplysningstype)) {
            null -> return emptySet()
            else ->
                regel.avhengerAv.map {
                    if (finnRegel(it) != null) {
                        trenger(it, fraDato)
                    } else {
                        setOf(it)
                    }
                }.flatten().filterNot { opplysninger.har(it, fraDato) }.toSet()
        }
    }

    private fun finnRegel(opplysningstype: Opplysningstype<*>): Regel<*>? {
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
