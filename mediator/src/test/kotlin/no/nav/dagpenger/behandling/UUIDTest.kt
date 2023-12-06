package no.nav.dagpenger.behandling

import com.fasterxml.uuid.Generators
import org.junit.jupiter.api.Test
import java.util.UUID

internal class UUIDTest {
    @Test
    fun `UUID is not null`() {
        val uuid = UUID.randomUUID().also { println(it) }

        val uuid2 = Generators.timeBasedGenerator().generate().also { println(it) }.toString()

        UUID.fromString(uuid2).also { println(it) }
    }
}
