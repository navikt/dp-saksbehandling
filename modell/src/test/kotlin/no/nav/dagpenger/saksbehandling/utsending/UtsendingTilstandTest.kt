package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import org.junit.jupiter.api.Test

class UtsendingTilstandTest {
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345678901"

    @Test
    fun `Lovlige tilstandsendringer`() {
        val meldingOmVedtak = "<html><body>Dette er et vedtaksbrev</body></html>"
        val utsending = Utsending(behandlingId = behandlingId, ident = ident, brev = meldingOmVedtak)
        utsending.tilstand() shouldBe Utsending.VenterPåVedtak

        utsending.startUtsending(
            StartUtsendingHendelse(
                behandlingId = behandlingId,
                ident = ident,
                utsendingSak = UtsendingSak(id = "sakId", kontekst = "fagsystem"),
            ),
        )
        utsending.tilstand() shouldBe Utsending.AvventerArkiverbarVersjonAvBrev
        utsending.tilstand().behov(utsending).shouldBeInstanceOf<ArkiverbartBrevBehov>()

        val pdfUrn = URN.rfc8141().parse("urn:pdf:123456")
        utsending.mottaUrnTilArkiverbartFormatAvBrev(ArkiverbartBrevHendelse(behandlingId = behandlingId, pdfUrn = pdfUrn))
        utsending.pdfUrn() shouldBe pdfUrn
        utsending.tilstand() shouldBe Utsending.AvventerJournalføring

        val journalførtHendelse = JournalførtHendelse(behandlingId = behandlingId, journalpostId = "123456")
        utsending.mottaJournalførtKvittering(journalførtHendelse)
        utsending.journalpostId() shouldBe journalførtHendelse.journalpostId
        utsending.tilstand() shouldBe Utsending.AvventerDistribuering

        val distribuertHendelse =
            DistribuertHendelse(
                behandlingId = behandlingId,
                distribusjonId = "distribueringId",
                journalpostId = "123456",
            )
        utsending.mottaDistribuertKvittering(distribuertHendelse)
        utsending.tilstand() shouldBe Utsending.Distribuert
    }

    @Test
    fun `Ugyldig tilstandsendring`() {
        val utsending = Utsending(behandlingId = behandlingId, ident = ident, brev = "html")

        shouldThrow<Utsending.Tilstand.UlovligUtsendingTilstandsendring> {
            utsending.mottaJournalførtKvittering(JournalførtHendelse(behandlingId = behandlingId, journalpostId = "123456"))
        }

        shouldThrow<Utsending.Tilstand.UlovligUtsendingTilstandsendring> {
            utsending.mottaUrnTilArkiverbartFormatAvBrev(
                ArkiverbartBrevHendelse(
                    behandlingId = behandlingId,
                    pdfUrn = "urn:pdf:123456".toUrn(),
                ),
            )
        }
    }
}
