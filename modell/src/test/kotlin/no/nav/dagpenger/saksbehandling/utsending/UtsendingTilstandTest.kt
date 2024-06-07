package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalpostHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.MidlertidigJournalpostHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.UtsendingKvitteringHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class UtsendingTilstandTest {
    @Test
    fun `Lovlige tilstandsendringer`() {
        val oppgaveId = UUIDv7.ny()
        val utsending = Utsending(oppgaveId = oppgaveId)
        utsending.brev() shouldBe null
        utsending.tilstand() shouldBe Utsending.Opprettet

        utsending.mottaBrev(VedtaksbrevHendelse(oppgaveId, brev = "Dette er et vedtaksbrev"))
        utsending.brev() shouldBe "Dette er et vedtaksbrev"
        utsending.tilstand() shouldBe Utsending.VenterPåVedtak

        utsending.startUtsending(StartUtsendingHendelse(oppgaveId, behandlingId = UUIDv7.ny(), ident = "12345678901"))
        utsending.tilstand() shouldBe Utsending.AvventerArkiverbarVersjonAvBrev
        utsending.tilstand().behov(utsending).shouldBeInstanceOf<ArkiverbartBrevBehov>()

        val pdfUrn = URN.rfc8141().parse("urn:pdf:123456")
        utsending.mottaUrnTilArkiverbartFormatAvBrev(ArkiverbartBrevHendelse(oppgaveId, pdfUrn = pdfUrn))
        utsending.pdfUrn() shouldBe pdfUrn
        utsending.tilstand() shouldBe Utsending.AvventerMidlertidigJournalføring

        val midlertidigJournalpostHendelse = MidlertidigJournalpostHendelse(oppgaveId, journalpostId = "123456")
        utsending.mottaMidlertidigJournalpost(midlertidigJournalpostHendelse)
        utsending.journalpostId() shouldBe midlertidigJournalpostHendelse.journalpostId
        utsending.tilstand() shouldBe Utsending.AvventerJournalføring

        val journalpostHendelse = JournalpostHendelse(oppgaveId, journalpostId = "123456")
        utsending.mottaJournalpost(journalpostHendelse)
        utsending.journalpostId() shouldBe journalpostHendelse.journalpostId
        utsending.tilstand() shouldBe Utsending.AvventerDistribuering

        val utsendingKvitteringHendelse = UtsendingKvitteringHendelse(utsendingId = UUIDv7.ny(), journalpostId = "123456")
        utsending.mottaKvitteringPåUtsending(utsendingKvitteringHendelse)
        utsending.tilstand() shouldBe Utsending.Distribuert
    }

    @Test
    @Disabled
    fun `Ugyldig tilstandsendring`() {
        val oppgaveId = UUIDv7.ny()
        val utsending = Utsending(oppgaveId = oppgaveId)
        val vedtaksbrevHendelse = VedtaksbrevHendelse(oppgaveId, brev = "Dette er et vedtaksbrev")
        shouldThrow<Utsending.Tilstand.UlovligUtsendingTilstandsendring> {
            utsending.mottaUrnTilArkiverbartFormatAvBrev(
                ArkiverbartBrevHendelse(oppgaveId, pdfUrn = "urn:pdf:123456".toUrn()),
            )
        }
        shouldThrow<Utsending.Tilstand.UlovligUtsendingTilstandsendring> {
            utsending.mottaBrev(vedtaksbrevHendelse)
        }
    }
}
