package no.nav.dagpenger.saksbehandling

import de.slub.urn.URN

data class Steg(val urn: URN, val opplysninger: List<Opplysning>)
