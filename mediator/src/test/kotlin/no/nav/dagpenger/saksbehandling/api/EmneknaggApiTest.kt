package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.api.MockAzure.Companion.autentisert
import no.nav.dagpenger.saksbehandling.api.OppgaveApiTestHelper.withOppgaveApi
import no.nav.dagpenger.saksbehandling.api.models.EmneknaggDTO
import no.nav.dagpenger.saksbehandling.api.models.EmneknaggKategoriDTO
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import org.junit.jupiter.api.Test

class EmneknaggApiTest {
    init {
        mockAzure()
    }

    @Test
    fun `GET emneknagger returnerer alle kodedefinerte emneknagger`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>(relaxed = true).also {
                every { it.hentDistinkteEmneknagger() } returns emptySet()
            }
        withOppgaveApi(oppgaveMediator = oppgaveMediator) {
            val response =
                client.get("/emneknagger") {
                    autentisert()
                }
            response.status shouldBe HttpStatusCode.OK
            val emneknagger = objectMapper.readValue(response.bodyAsText(), Array<EmneknaggDTO>::class.java).toList()
            emneknagger shouldHaveAtLeastSize Emneknagg.alleKodedefinerte.size
        }
    }

    @Test
    fun `GET emneknagger inkluderer ukjente DB-emneknagger som UDEFINERT`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>(relaxed = true).also {
                every { it.hentDistinkteEmneknagger() } returns setOf("Ukjent knagg fra DB")
            }
        withOppgaveApi(oppgaveMediator = oppgaveMediator) {
            val response =
                client.get("/emneknagger") {
                    autentisert()
                }
            response.status shouldBe HttpStatusCode.OK
            val emneknagger = objectMapper.readValue(response.bodyAsText(), Array<EmneknaggDTO>::class.java).toList()
            emneknagger shouldContainAll
                listOf(
                    EmneknaggDTO(visningsnavn = "Ukjent knagg fra DB", kategori = EmneknaggKategoriDTO.UDEFINERT),
                )
        }
    }

    @Test
    fun `GET emneknagger filtrerer bort Ettersending`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>(relaxed = true).also {
                every { it.hentDistinkteEmneknagger() } returns setOf("Ettersending(2026-05-12)")
            }
        withOppgaveApi(oppgaveMediator = oppgaveMediator) {
            val response =
                client.get("/emneknagger") {
                    autentisert()
                }
            response.status shouldBe HttpStatusCode.OK
            val visningsnavn =
                objectMapper
                    .readValue(response.bodyAsText(), Array<EmneknaggDTO>::class.java)
                    .map { it.visningsnavn }
            visningsnavn shouldNotContain "Ettersending(2026-05-12)"
        }
    }

    @Test
    fun `GET emneknagger dedupliserer DB-emneknagger som allerede er kodedefinert`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>(relaxed = true).also {
                every { it.hentDistinkteEmneknagger() } returns setOf("Avslag", "Innvilgelse")
            }
        withOppgaveApi(oppgaveMediator = oppgaveMediator) {
            val response =
                client.get("/emneknagger") {
                    autentisert()
                }
            response.status shouldBe HttpStatusCode.OK
            val emneknagger = objectMapper.readValue(response.bodyAsText(), Array<EmneknaggDTO>::class.java).toList()
            val avslagEntries = emneknagger.filter { it.visningsnavn == "Avslag" }
            avslagEntries shouldHaveAtLeastSize 1
            avslagEntries.size shouldBe 1
        }
    }

    @Test
    fun `GET emneknagger returnerer sortert liste`() {
        val oppgaveMediator =
            mockk<OppgaveMediator>(relaxed = true).also {
                every { it.hentDistinkteEmneknagger() } returns emptySet()
            }
        withOppgaveApi(oppgaveMediator = oppgaveMediator) {
            val response =
                client.get("/emneknagger") {
                    autentisert()
                }
            val emneknagger = objectMapper.readValue(response.bodyAsText(), Array<EmneknaggDTO>::class.java).toList()
            val visningsnavn = emneknagger.map { it.visningsnavn }
            visningsnavn shouldBe visningsnavn.sorted()
        }
    }
}

class ByggEmneknaggListeTest {
    @Test
    fun `kodedefinerte emneknagger inkluderes med riktig kategori`() {
        val resultat = byggEmneknaggListe(emptySet())
        resultat shouldContainAll
            listOf(
                EmneknaggDTO(visningsnavn = "Avslag", kategori = EmneknaggKategoriDTO.SOKNADSRESULTAT),
                EmneknaggDTO(visningsnavn = "Innvilgelse", kategori = EmneknaggKategoriDTO.SOKNADSRESULTAT),
                EmneknaggDTO(visningsnavn = "Retur fra kontroll", kategori = EmneknaggKategoriDTO.UDEFINERT),
                EmneknaggDTO(visningsnavn = "D-nummer", kategori = EmneknaggKategoriDTO.UDEFINERT),
            )
    }

    @Test
    fun `ukjente DB-emneknagger blir UDEFINERT`() {
        val resultat = byggEmneknaggListe(setOf("Helt ny knagg"))
        resultat shouldContainAll
            listOf(
                EmneknaggDTO(visningsnavn = "Helt ny knagg", kategori = EmneknaggKategoriDTO.UDEFINERT),
            )
    }

    @Test
    fun `Ettersending filtreres bort`() {
        val resultat = byggEmneknaggListe(setOf("Ettersending(2026-01-01)"))
        resultat.map { it.visningsnavn } shouldNotContain "Ettersending(2026-01-01)"
    }

    @Test
    fun `duplikater fra DB gir ikke doble entries`() {
        val resultat = byggEmneknaggListe(setOf("Avslag"))
        resultat.filter { it.visningsnavn == "Avslag" }.size shouldBe 1
    }
}
