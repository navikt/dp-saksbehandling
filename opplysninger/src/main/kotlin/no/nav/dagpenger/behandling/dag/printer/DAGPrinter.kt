package no.nav.dagpenger.behandling.dag.printer

import no.nav.dagpenger.behandling.Opplysningstype

interface DAGPrinter {
    fun toPrint(root: Opplysningstype<Boolean>? = null): String
}
