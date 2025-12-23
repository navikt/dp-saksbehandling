package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon.IngenAksjon
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon.OversendKlageinstans
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_1
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_NAVN
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HJEMLER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HVEM_KLAGER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAK
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.Verdi.Flervalg
import no.nav.dagpenger.saksbehandling.klage.Verdi.TekstVerdi
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KlageEdgeCasesTest {
    private val saksbehandler =
        Saksbehandler(
            navIdent = "saksbehandler",
            grupper = setOf("SaksbehandlerADGruppe"),
            tilganger = setOf(SAKSBEHANDLER),
        )

    @Test
    fun `Håndterer manglende opprinnelig vedtak journalpostId gracefully`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it.svar(it.finnOpplysningId(KLAGEN_GJELDER_VEDTAK), TekstVerdi(UUID.randomUUID().toString()))
            }

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { null },
            ) as OversendKlageinstans

        aksjon.tilknyttedeJournalposter.size shouldBe 1
        aksjon.tilknyttedeJournalposter[0].type shouldBe "KLAGEMELDING"
        aksjon.tilknyttedeJournalposter[0].journalpostId shouldBe "jp-vedtak-ny"
    }

    @Test
    fun `Håndterer tom hjemler liste`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it.svar(it.finnOpplysningId(HJEMLER), Flervalg(emptyList()))
            }

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { null },
            ) as OversendKlageinstans

        aksjon.hjemler shouldBe emptyList()
    }

    @Test
    fun `Håndterer ufullstendig fullmektig data når HVEM_KLAGER er FULLMEKTIG`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it.svar(it.finnOpplysningId(HVEM_KLAGER), Flervalg(listOf(HvemKlagerType.FULLMEKTIG.name)))
                it.svar(it.finnOpplysningId(FULLMEKTIG_NAVN), TekstVerdi("Advokat Hansen"))
                it.svar(it.finnOpplysningId(FULLMEKTIG_ADRESSE_1), TekstVerdi(""))
            }

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { null },
            ) as OversendKlageinstans

        aksjon.fullmektigData["fullmektigNavn"] shouldBe "Advokat Hansen"
        aksjon.fullmektigData["fullmektigAdresse1"] shouldBe ""
    }

    @Test
    fun `Håndterer flere tilknyttede journalposter korrekt`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it.svar(it.finnOpplysningId(KLAGEN_GJELDER_VEDTAK), TekstVerdi(UUID.randomUUID().toString()))
            }

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { "jp-original-vedtak" },
            ) as OversendKlageinstans

        aksjon.tilknyttedeJournalposter.size shouldBe 2
        aksjon.tilknyttedeJournalposter.any { it.journalpostId == "jp-original-vedtak" && it.type == "VEDTAK" } shouldBe true
        aksjon.tilknyttedeJournalposter.any { it.journalpostId == "jp-vedtak-ny" && it.type == "KLAGEMELDING" } shouldBe true
    }

    @Test
    fun `vedtakDistribuert med null behandlendeEnhet kaster IllegalStateException`() {
        val klageBehandling = lagKlageBehandlingMedUtfall(UtfallType.OPPRETTHOLDT)

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        shouldThrow<IllegalStateException> {
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { null },
            )
        }
    }

    @Test
    fun `vedtakDistribuert med utfall DELVIS_MEDHOLD returnerer IngenAksjon`() {
        val klageBehandling = lagKlageBehandlingMedUtfall(UtfallType.DELVIS_MEDHOLD)

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { null },
            )

        aksjon.shouldBeInstanceOf<IngenAksjon>()
    }

    @Test
    fun `vedtakDistribuert uten KLAGEN_GJELDER_VEDTAK gir kun klagemelding journalpost`() {
        val klageBehandling = lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450")

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { "jp-original-vedtak" },
            ) as OversendKlageinstans

        aksjon.tilknyttedeJournalposter.size shouldBe 1
        aksjon.tilknyttedeJournalposter[0].type shouldBe "KLAGEMELDING"
    }

    @Test
    fun `Callback får ikke kalles hvis hentVedtakIdBrukerKlagerPå returnerer null`() {
        val klageBehandling = lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450")

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        var callbackCalled = false
        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = {
                    callbackCalled = true
                    "jp-original-vedtak"
                },
            ) as OversendKlageinstans

        callbackCalled shouldBe false
        aksjon.tilknyttedeJournalposter.size shouldBe 1
    }

    @Test
    fun `Flere utfall verdier i UTFALL opplysning bruker første verdi`() {
        val klageBehandling = KlageBehandling(opprettet = LocalDateTime.now())
        val utfallOpplysningId = klageBehandling.finnOpplysningId(UTFALL)
        klageBehandling.svar(utfallOpplysningId, Flervalg(listOf(UtfallType.OPPRETTHOLDT.name, UtfallType.AVVIST.name)))
        klageBehandling.behandlingUtført(
            behandlendeEnhet = "4450",
            hendelse =
                KlageBehandlingUtført(
                    behandlingId = klageBehandling.behandlingId,
                    utførtAv = saksbehandler,
                ),
        )

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { null },
            )

        aksjon.shouldBeInstanceOf<OversendKlageinstans>()
    }

    private fun lagKlageBehandlingMedUtfall(utfall: UtfallType): KlageBehandling {
        val klageBehandling = KlageBehandling(opprettet = LocalDateTime.now())
        val utfallOpplysningId = klageBehandling.finnOpplysningId(UTFALL)
        klageBehandling.svar(utfallOpplysningId, Flervalg(listOf(utfall.name)))
        return klageBehandling
    }

    private fun lagKlageBehandlingMedUtfallOgEnhet(
        utfall: UtfallType,
        behandlendeEnhet: String,
    ): KlageBehandling {
        val klageBehandling = KlageBehandling(opprettet = LocalDateTime.now())
        val utfallOpplysningId = klageBehandling.finnOpplysningId(UTFALL)
        klageBehandling.svar(utfallOpplysningId, Flervalg(listOf(utfall.name)))
        klageBehandling.behandlingUtført(
            behandlendeEnhet = behandlendeEnhet,
            hendelse =
                KlageBehandlingUtført(
                    behandlingId = klageBehandling.behandlingId,
                    utførtAv = saksbehandler,
                ),
        )
        return klageBehandling
    }

    private fun KlageBehandling.finnOpplysningId(type: OpplysningType): UUID =
        this.alleOpplysninger().first { it.opplysningType == type }.opplysningId
}
