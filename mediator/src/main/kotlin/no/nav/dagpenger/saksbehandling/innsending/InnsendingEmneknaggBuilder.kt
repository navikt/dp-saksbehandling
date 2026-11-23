package no.nav.dagpenger.saksbehandling.innsending

import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse

internal fun InnsendingMottattHendelse.emneknaggForSkjemaKode(): Set<String> =
    when (this.skjemaKode) {
        "T6" -> setOf("Permitteringsvarsel")
        "T3" -> setOf("Tjenestebevis")
        "T4" -> setOf("Oppholdstillatelse")
        "NAV 04-08.03" -> setOf("NAV 04-08.03")
        "NAV 04-08.04" -> setOf("NAV 04-08.04")
        "NAV 90-00.08 A" -> setOf("Klage/anke")
        "NAV 90-00.08" -> setOf("Klage/anke")
        "NAV 04-06.05" -> setOf("Tar utdanning")
        "NAV 04-06.08" -> setOf("Etablering")
        "NAV 04-02.05" -> setOf("EØS")
        "NAV 04-02.01" -> setOf("EØS")
        "NAV 04-13.01" -> setOf("Grensearbeider")
        "XY" -> setOf("Faguttalelse")
        else -> emptySet()
    }
