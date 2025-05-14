package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class KlageKlientMapperTest {
    @Test
    fun `ingen hjemler i klagebehandling fører til tom liste av hjemler`() {
        val klageBehandling =
            lagKlagebehandling(
                hjemler = listOf(),
            )
        klageBehandling.hjemler() shouldBe emptyList()
    }

    @Test
    fun `henter hjemler fra klagebehandling`() {
        val klageBehandling =
            lagKlagebehandling(
                hjemler = listOf(Hjemler.FTRL_4_7_PERMITTERINGENS_LENGDE, Hjemler.FTRL_4_2),
            )
        klageBehandling.hjemler() shouldBe listOf(Hjemler.FTRL_4_7_PERMITTERINGENS_LENGDE, Hjemler.FTRL_4_2)
    }

    @Test
    fun `ukjent land blir ??? i prosessfullmektig`() {
        val klageBehandling = lagKlagebehandling(land = Land.UKJENT)
        klageBehandling.prosessFullmektig()?.adresse?.land shouldBe "???"
    }

    @Test
    fun `kjent land blir landkode i prosessfullmektig`() {
        val klageBehandling = lagKlagebehandling(land = Land.FR)
        klageBehandling.prosessFullmektig()?.adresse?.land shouldBe "FR"
    }

    @Test
    fun `fullmektig uten land har ikke adresse`() {
        val klageBehandling = lagKlagebehandling(land = null)
        klageBehandling.prosessFullmektig()?.navn shouldBe "Djevelens Advokat"
        klageBehandling.prosessFullmektig()?.adresse shouldBe null
    }

    @Test
    fun `fullmektig med land har adresse`() {
        val klageBehandling = lagKlagebehandling(land = Land.NO)
        klageBehandling.prosessFullmektig()?.adresse?.land shouldBe "NO"
        klageBehandling.prosessFullmektig()?.adresse?.poststed shouldBe "Oslo"
        klageBehandling.prosessFullmektig()?.adresse?.postnummer shouldBe "0666"
        klageBehandling.prosessFullmektig()?.adresse?.addresselinje1 shouldBe "Sydenveien 1"
        klageBehandling.prosessFullmektig()?.adresse?.addresselinje2 shouldBe "Poste restante"
        klageBehandling.prosessFullmektig()?.adresse?.addresselinje3 shouldBe "Teisen postkontor"
    }
}
