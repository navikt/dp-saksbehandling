package no.nav.dagpenger.saksbehandling.utsending

import de.slub.urn.URN
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.VedtaksbrevHendelse
import org.junit.jupiter.api.Test

class UtsendingTilstandTest {
    private val oppgaveId = UUIDv7.ny()
    private val ident = "12345678901"

    @Test
    fun `Lovlige tilstandsendringer`() {
        val utsending = Utsending(oppgaveId = oppgaveId, ident = ident)
        utsending.brev() shouldBe null
        utsending.tilstand() shouldBe Utsending.Opprettet

        utsending.mottaBrev(VedtaksbrevHendelse(oppgaveId, brev = "Dette er et vedtaksbrev"))
        utsending.brev() shouldBe "Dette er et vedtaksbrev"
        utsending.tilstand() shouldBe Utsending.VenterPåVedtak

        utsending.startUtsending(
            StartUtsendingHendelse(
                oppgaveId = oppgaveId,
                behandlingId = UUIDv7.ny(),
                ident = ident,
                sak = Sak(id = "sakId", kontekst = "fagsystem"),
            ),
        )
        utsending.tilstand() shouldBe Utsending.AvventerArkiverbarVersjonAvBrev
        utsending.tilstand().behov(utsending).shouldBeInstanceOf<ArkiverbartBrevBehov>()

        val pdfUrn = URN.rfc8141().parse("urn:pdf:123456")
        utsending.mottaUrnTilArkiverbartFormatAvBrev(ArkiverbartBrevHendelse(oppgaveId, pdfUrn = pdfUrn))
        utsending.pdfUrn() shouldBe pdfUrn
        utsending.tilstand() shouldBe Utsending.AvventerJournalføring

        val journalførtHendelse = JournalførtHendelse(oppgaveId, journalpostId = "123456")
        utsending.mottaJournalførtKvittering(journalførtHendelse)
        utsending.journalpostId() shouldBe journalførtHendelse.journalpostId
        utsending.tilstand() shouldBe Utsending.AvventerDistribuering

        val distribuertHendelse =
            DistribuertHendelse(
                oppgaveId = oppgaveId,
                distribusjonId = "distribueringId",
                journalpostId = "123456",
            )
        utsending.mottaDistribuertKvittering(distribuertHendelse)
        utsending.tilstand() shouldBe Utsending.Distribuert
    }

    @Test
    fun `Ugyldig tilstandsendring`() {
        val utsending = Utsending(oppgaveId = oppgaveId, ident = ident)

        shouldThrow<Utsending.Tilstand.UlovligUtsendingTilstandsendring> {
            utsending.mottaJournalførtKvittering(JournalførtHendelse(oppgaveId, journalpostId = "123456"))
        }

        shouldThrow<Utsending.Tilstand.UlovligUtsendingTilstandsendring> {
            utsending.mottaUrnTilArkiverbartFormatAvBrev(
                ArkiverbartBrevHendelse(
                    oppgaveId,
                    pdfUrn = "urn:pdf:123456".toUrn(),
                ),
            )
        }
    }
}
