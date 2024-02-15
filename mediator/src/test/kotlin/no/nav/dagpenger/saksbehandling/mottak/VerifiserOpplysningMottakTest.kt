package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Mediator
import no.nav.dagpenger.saksbehandling.hendelser.VerifiserOpplysningHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VerifiserOpplysningMottakTest {
    private val testRapid = TestRapid()
    private val mediator = mockk<Mediator>(relaxed = true)
    val behandlingId = UUID.randomUUID()
    val ident = "12345678901"
    private val verifiserOpplysningHendelse =
        VerifiserOpplysningHendelse(
            behandlingId = behandlingId,
            ident = ident,
        )

    init {
        VerifiserOpplysningMottak(testRapid, mediator)
    }

    @Test
    fun `Skal behandle verifiser_opplysning hendelse`() {
        testRapid.sendTestMessage(verifiserOpplysningMelding)
        verify(exactly = 1) {
            mediator.behandle(verifiserOpplysningHendelse)
        }
    }

    @Language("JSON")
    private val verifiserOpplysningMelding =
        """
        {
            "@event_name": "verifiser_opplysning",
            "@opprettet": "2024-01-30T10:43:32.988331190",
            "@id": "9fca5cad-d6fa-4296-a057-1c5bb04cdaac",
            "behandlingId": "$behandlingId",
            "ident": "$ident"
        }
        """
}
