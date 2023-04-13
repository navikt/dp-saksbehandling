package no.nav.dagpenger.behandling

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SøknadBehandletTest {
    private val jackson = jacksonObjectMapper()

    @Test
    fun `Skal lage søknad_behandlet_hendelse`() {
        val behandlingId = UUID.randomUUID()
        val ident = "1234567890"

        SøknadBehandlet(
            behandlingId = behandlingId,
            ident = ident,
            innvilget = true,
        ).toJson().also {
            val jsonNode = jackson.readTree(it)

            jsonNode["@event_name"].asText() shouldBe "søknad_behandlet_hendelse"
            jsonNode["behandlingId"].asText() shouldBe behandlingId.toString()
            jsonNode["virkningsdato"] shouldNotBe null
            jsonNode["innvilget"].asText() shouldBe "true"
            jsonNode["ident"].asText() shouldBe ident
            jsonNode["@id"] shouldNotBe null
            jsonNode["@opprettet"] shouldNotBe null
        }
    }
}
