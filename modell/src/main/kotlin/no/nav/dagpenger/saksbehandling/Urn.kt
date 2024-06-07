package no.nav.dagpenger.saksbehandling

import de.slub.urn.URN

fun String?.toUrnOrNull(): URN? = this?.toUrn()

fun String.toUrn(): URN = URN.rfc8141().parse(this)
