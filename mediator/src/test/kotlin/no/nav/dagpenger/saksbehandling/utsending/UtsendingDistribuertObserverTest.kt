package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.helper.arkiverbartDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.distribuertDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.journalføringBehovLøsning
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingBehovLøsningMottak
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UtsendingDistribuertObserverTest {
    private val rapid = TestRapid()

    @Test
    fun `skal publisere melding når utsending er distribuert`() {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                utløstAv = UtløstAvType.SØKNAD,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )
        val person = TestHelper.testPerson

        DBTestHelper.withBehandling(behandling = behandling, person = person) { ds ->
            val behandlingId = behandling.behandlingId
            val sakId = DBTestHelper.sakId.toString()
            val utsendingSak = UtsendingSak(sakId, "Dagpenger")
            val htmlBrev = "<H1>Hei</H1><p>Her er et brev</p>"
            val utsendingRepository = PostgresUtsendingRepository(ds)
            val utsendingMediator =
                UtsendingMediator(
                    utsendingRepository = utsendingRepository,
                    brevProdusent =
                        mockk<UtsendingMediator.BrevProdusent>().also {
                            coEvery {
                                it.lagBrev(
                                    ident = person.ident,
                                    behandlingId = behandlingId,
                                    sakId = sakId,
                                )
                            } returns htmlBrev
                        },
                ).also {
                    it.setRapidsConnection(rapid)
                    it.addObserver(UtsendingDistribuertObserver(rapid))
                }

            UtsendingBehovLøsningMottak(
                utsendingMediator = utsendingMediator,
                rapidsConnection = rapid,
            )

            val utsendingId =
                utsendingMediator.opprettUtsending(
                    behandlingId = behandlingId,
                    brev = null,
                    ident = person.ident,
                    type = UtsendingType.VEDTAK_DAGPENGER,
                )

            // Start utsending
            utsendingMediator.mottaStartUtsending(
                StartUtsendingHendelse(
                    behandlingId = behandlingId,
                    ident = person.ident,
                    brev = htmlBrev,
                    utsendingSak = utsendingSak,
                ),
            )

            // Arkiverbart brev
            val pdfUrn = "urn:vedlegg:123".toUrn()
            rapid.sendTestMessage(
                arkiverbartDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    pdfUrnString = pdfUrn.toString(),
                ),
            )

            // Journalført
            val journalpostId = "123456"
            rapid.sendTestMessage(
                journalføringBehovLøsning(
                    behandlingId = behandlingId,
                    journalpostId = journalpostId,
                ),
            )

            rapid.reset()

            // Distribuert
            val distribusjonId = "789012"
            rapid.sendTestMessage(
                distribuertDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    journalpostId = journalpostId,
                    distribusjonId = distribusjonId,
                    ident = person.ident,
                    utsendingId = utsendingId,
                ),
            )

            rapid.inspektør.size shouldBe 1

            val expectedMessage =
                """
                {
                  "@event_name": "utsending_distribuert",
                  "behandlingId": "$behandlingId",
                  "utsendingId": "$utsendingId",
                  "distribusjonId": "$distribusjonId",
                  "journalpostId": "$journalpostId",
                  "ident": "${person.ident}",
                  "type": "VEDTAK_DAGPENGER"
                }
                """.trimIndent()

            rapid.inspektør.message(0).toString() shouldEqualSpecifiedJson expectedMessage
        }
    }
}
