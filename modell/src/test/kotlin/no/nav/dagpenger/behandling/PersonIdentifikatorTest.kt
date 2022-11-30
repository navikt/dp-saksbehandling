package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.PersonIdentifikator.Companion.tilPersonIdentfikator
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PersonIdentifikatorTest {

    @Test
    fun `personidentifikator består av 11 siffer`() {
        assertDoesNotThrow { "12345678901".tilPersonIdentfikator() }
        assertThrows<IllegalArgumentException> { "123".tilPersonIdentfikator() }
        assertThrows<IllegalArgumentException> { "ident".tilPersonIdentfikator() }
    }

    @Test
    fun ` likhet `() {
        val personIdent = "12345678901".tilPersonIdentfikator()
        assertEquals(personIdent, personIdent)
        assertEquals(personIdent, "12345678901".tilPersonIdentfikator())
        assertNotEquals(personIdent, "22345678901".tilPersonIdentfikator())
        assertNotEquals("22345678901".tilPersonIdentfikator(), personIdent)
        assertNotEquals(personIdent, Any())
        assertNotEquals(personIdent, null)
    }
}
