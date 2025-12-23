package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldHaveSize
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
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingBehovLøsningMottak
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UtsendingHendelseObserverTest {
    private val rapid = TestRapid()

    @Test
    fun `observer skal få beskjed om alle hendelser i utsendingsprosessen`() {
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
                }

            UtsendingBehovLøsningMottak(
                utsendingMediator = utsendingMediator,
                rapidsConnection = rapid,
            )

            val testObserver = TestUtsendingObserver()
            utsendingMediator.addObserver(testObserver)

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

            testObserver.startUtsendingHendelser shouldHaveSize 1
            testObserver.startUtsendingHendelser.first().behandlingId shouldBe behandlingId

            // Arkiverbart brev
            val pdfUrn = "urn:vedlegg:123".toUrn()
            rapid.sendTestMessage(
                arkiverbartDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    pdfUrnString = pdfUrn.toString(),
                ),
            )

            testObserver.arkiverbartBrevHendelser shouldHaveSize 1
            testObserver.arkiverbartBrevHendelser.first().behandlingId shouldBe behandlingId

            // Journalført
            val journalpostId = "123"
            rapid.sendTestMessage(
                journalføringBehovLøsning(
                    behandlingId = behandlingId,
                    journalpostId = journalpostId,
                ),
            )

            testObserver.journalførtHendelser shouldHaveSize 1
            testObserver.journalførtHendelser.first().behandlingId shouldBe behandlingId

            // Distribuert
            rapid.sendTestMessage(
                distribuertDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    journalpostId = journalpostId,
                    distribusjonId = "456",
                ),
            )

            testObserver.distribuertHendelser shouldHaveSize 1
            testObserver.distribuertHendelser.first().behandlingId shouldBe behandlingId
        }
    }

    private class TestUtsendingObserver : UtsendingHendelseObserver {
        val startUtsendingHendelser = mutableListOf<StartUtsendingHendelse>()
        val arkiverbartBrevHendelser = mutableListOf<ArkiverbartBrevHendelse>()
        val journalførtHendelser = mutableListOf<JournalførtHendelse>()
        val distribuertHendelser = mutableListOf<DistribuertHendelse>()

        override fun onStartUtsending(
            hendelse: StartUtsendingHendelse,
            utsending: Utsending,
        ) {
            startUtsendingHendelser.add(hendelse)
        }

        override fun onArkiverbartBrev(
            hendelse: ArkiverbartBrevHendelse,
            utsending: Utsending,
        ) {
            arkiverbartBrevHendelser.add(hendelse)
        }

        override fun onJournalført(
            hendelse: JournalførtHendelse,
            utsending: Utsending,
        ) {
            journalførtHendelser.add(hendelse)
        }

        override fun onDistribuert(
            hendelse: DistribuertHendelse,
            utsending: Utsending,
        ) {
            distribuertHendelser.add(hendelse)
        }
    }
}
