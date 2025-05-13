package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.prometheus.metrics.model.registry.PrometheusRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class KlageHttpKlientTest {
    @Test
    fun `Oversend klage til klageinstans`() {
        val klageBehandling = lagKlagebehandling()
        
        // TODO: hvordan konvertere hjemler som opplysning til hjemler som liste av Hjemler?
        val hjemler =
            klageBehandling.alleOpplysninger().single { it.type == OpplysningType.HJEMLER }.verdi()
                .toString()
                .removeSurrounding("Flervalg(value=[", "])")
                .split(",")
                .map { Hjemler.valueOf(it.trim()) }
        val oppgave =
            lagOppgave(
                behandling =
                    lagBehandling(
                        behandlingId = klageBehandling.behandlingId,
                        type = BehandlingType.KLAGE,
                    ),
                opprettet = LocalDateTime.now(),
            )
        val mockEngine =
            MockEngine { request ->
                request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
                request.url.host shouldBe "localhost"
                request.url.segments shouldBe listOf("api", "oversendelse", "v4", "sak")
                respondOk("")
            }

        val kabalKlient =
            KlageHttpKlient(
                kabalApiUrl = "http://localhost:8080",
                tokenProvider = { "token" },
                httpClient =
                    httpClient(
                        prometheusRegistry = PrometheusRegistry(),
                        engine = mockEngine,
                    ),
            )
        val resultat: Result<HttpStatusCode> =
            runBlocking {
                kabalKlient.registrerKlage(
                    personIdentId = oppgave.behandling.person.ident,
                    fagsakId = UUIDv7.ny().toString(),
                    behandlingId = klageBehandling.behandlingId,
                    forrigeBehandlendeEnhet = "4450",
                    tilknyttedeJournalposter = listOf(),
                    hjemler = hjemler,
                )
            }
        resultat shouldBe Result.success(HttpStatusCode.OK)
    }

    private fun lagKlagebehandling(): KlageBehandling {
        return KlageBehandling(
            opplysninger =
                setOf(
                    Opplysning(
                        type = OpplysningType.KLAGEN_GJELDER,
                        verdi = Verdi.Flervalg(listOf("Avslag på søknad")),
                    ),
                    Opplysning(
                        type = OpplysningType.KLAGEN_GJELDER_VEDTAK,
                        verdi = Verdi.TekstVerdi("Vedtak 1"),
                    ),
                    Opplysning(
                        type = OpplysningType.ER_KLAGEN_SKRIFTLIG,
                        verdi = Verdi.Boolsk(true),
                    ),
                    Opplysning(
                        type = OpplysningType.ER_KLAGEN_UNDERSKREVET,
                        verdi = Verdi.Boolsk(true),
                    ),
                    Opplysning(
                        type = OpplysningType.KLAGEN_NEVNER_ENDRING,
                        verdi = Verdi.Boolsk(true),
                    ),
                    Opplysning(
                        type = OpplysningType.RETTSLIG_KLAGEINTERESSE,
                        verdi = Verdi.Boolsk(true),
                    ),
                    Opplysning(
                        type = OpplysningType.KLAGE_MOTTATT,
                        verdi = Verdi.Dato(LocalDate.of(2025, 1, 1)),
                    ),
                    Opplysning(
                        type = OpplysningType.KLAGEFRIST,
                        verdi = Verdi.Dato(LocalDate.of(2025, 1, 7)),
                    ),
                    Opplysning(
                        type = OpplysningType.KLAGEFRIST_OPPFYLT,
                        verdi = Verdi.Boolsk(true),
                    ),
                    Opplysning(
                        type = OpplysningType.UTFALL,
                        verdi = Verdi.TekstVerdi("OPPRETTHOLDELSE"),
                    ),
                    Opplysning(
                        type = OpplysningType.VURDERING_AV_KLAGEN,
                        verdi = Verdi.TekstVerdi("Veldig fin klage :)"),
                    ),
                    Opplysning(
                        type = OpplysningType.HVEM_KLAGER,
                        verdi = Verdi.TekstVerdi("FULLMEKTIG"),
                    ),
                    Opplysning(
                        type = OpplysningType.HJEMLER,
                        verdi = Verdi.Flervalg("FTRL_4_2", "FTRL_4_9", "FTRL_4_18"),
                    ),
                    Opplysning(
                        type = OpplysningType.INTERN_MELDING,
                        verdi = Verdi.TekstVerdi("Kuleste klagen jeg noensinne har sett"),
                    ),
                    Opplysning(
                        type = OpplysningType.FULLMEKTIG_NAVN,
                        verdi = Verdi.TekstVerdi("Djevelens Advokat"),
                    ),
                    Opplysning(
                        type = OpplysningType.FULLMEKTIG_ADRESSE_1,
                        verdi = Verdi.TekstVerdi("Sydenveien 1"),
                    ),
                    Opplysning(
                        type = OpplysningType.FULLMEKTIG_POSTNR,
                        verdi = Verdi.TekstVerdi("0666"),
                    ),
                    Opplysning(
                        type = OpplysningType.FULLMEKTIG_POSTSTED,
                        verdi = Verdi.TekstVerdi("Oslo"),
                    ),
                    Opplysning(
                        type = OpplysningType.FULLMEKTIG_LAND,
                        verdi = Verdi.TekstVerdi("NO"),
                    ),
                ),
        )
    }
}
