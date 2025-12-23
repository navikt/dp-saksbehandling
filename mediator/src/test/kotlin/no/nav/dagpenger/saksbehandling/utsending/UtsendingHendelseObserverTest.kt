package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import de.slub.urn.URN
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class UtsendingHendelseObserverTest {
    @Test
    fun `observer skal få beskjed om alle hendelser i utsendingsprosessen`() {
        val rapid = TestRapid()
        val behandlingId = UUID.randomUUID()
        val utsendingSak = UtsendingSak(kontekst = "Dagpenger", id = "123456789")
        val utsending =
            TestHelper.lagUtsending(
                tilstand = Utsending.VenterPåVedtak,
                behandlingId = behandlingId,
                utsendingSak = utsendingSak,
            )
        val testObserver = TestUtsendingObserver()
        val utsendingRepository =
            object : UtsendingRepository {
                private val utsendinger = mutableSetOf<Utsending>(utsending)

                override fun lagre(utsending: Utsending) {
                    utsendinger.add(utsending)
                }

                override fun utsendingFinnesForBehandling(behandlingId: UUID): Boolean = utsendinger.any { it.behandlingId == behandlingId }

                override fun slettUtsending(utsendingId: UUID): Int = utsendinger.removeIf { it.id == utsendingId }.let { if (it) 1 else 0 }

                override fun finnUtsendingForBehandlingId(behandlingId: UUID): Utsending? =
                    utsendinger.find {
                        it.behandlingId ==
                            behandlingId
                    }

                override fun hentUtsendingForBehandlingId(behandlingId: UUID): Utsending =
                    utsendinger.first {
                        it.behandlingId ==
                            behandlingId
                    }
            }
        val mediator =
            UtsendingMediator(
                utsendingRepository = utsendingRepository,
                brevProdusent = mockk(),
            ).also {
                it.setRapidsConnection(rapid)
                it.addObserver(testObserver)
            }

        val startUtsendHendelse =
            StartUtsendingHendelse(
                behandlingId = behandlingId,
                utsendingSak = utsending.sak()!!,
                ident = utsending.ident,
                brev = "Dette er et testbrev",
            )
        mediator.mottaStartUtsending(startUtsendHendelse)
        testObserver.startUtsendingHendelser.single() shouldBe startUtsendHendelse

        val arkiverbartBrevHendelse =
            ArkiverbartBrevHendelse(
                behandlingId = behandlingId,
                pdfUrn = URN.rfc8141().parse("urn:hubba:bubba"),
            )
        mediator.mottaUrnTilArkiverbartFormatAvBrev(
            arkiverbartBrevHendelse,
        )
        testObserver.arkiverbartBrevHendelser.single() shouldBe arkiverbartBrevHendelse

        val journalførtHendelse =
            JournalførtHendelse(
                behandlingId = behandlingId,
                journalpostId = "journalpost-123",
            )
        mediator.mottaJournalførtKvittering(journalførtHendelse)
        testObserver.journalførtHendelser.single() shouldBe journalførtHendelse

        val distribuertHendelse =
            DistribuertHendelse(
                behandlingId = behandlingId,
                distribusjonId = "distribusjon-456",
                journalpostId = "journalpost-123",
            )
        mediator.mottaDistribuertKvittering(distribuertHendelse)
        testObserver.distribuertHendelser.single() shouldBe distribuertHendelse
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
