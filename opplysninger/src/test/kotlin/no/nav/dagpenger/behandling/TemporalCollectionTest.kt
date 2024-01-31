package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TemporalCollectionTest {
    private lateinit var satser: TemporalCollection<BigDecimal>
    private val lavSats = BigDecimal(5)
    private val høySats = BigDecimal(10)

    @BeforeEach
    fun setUp() {
        satser = TemporalCollection()
        satser.put(1.mars, lavSats)
        satser.put(4.juli, høySats)
    }

    @Test
    fun `får riktig sats til riktig dato`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            satser.get(1.januar)
        }
        assertEquals(lavSats, satser.get(1.mars))
        assertEquals(lavSats, satser.get(1.juli))
        assertEquals(høySats, satser.get(4.juli))
        assertEquals(høySats, satser.get(15.juli))
    }

    @Test
    fun `Satt på samme dato`() {
        satser.put(7.juli, lavSats)
        satser.put(7.juli, BigDecimal(20))

        assertEquals(BigDecimal(20), satser.get(7.juli))
    }
}
