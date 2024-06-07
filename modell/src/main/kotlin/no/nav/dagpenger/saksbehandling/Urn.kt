package no.nav.dagpenger.saksbehandling

import de.slub.urn.URN
import de.slub.urn.URN_8141

fun String.toUrn(): URN_8141 = URN.rfc8141().parse(this)
