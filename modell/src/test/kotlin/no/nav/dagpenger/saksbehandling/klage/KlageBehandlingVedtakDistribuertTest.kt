package no.nav.dagpenger.saksbehandling.klage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon.IngenAksjon
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon.OversendKlageinstans
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.OVERSEND_KLAGEINSTANS
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_1
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_2
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_3
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_LAND
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_NAVN
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_POSTNR
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_POSTSTED
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HJEMLER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HVEM_KLAGER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.INTERN_MELDING
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAK
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.Verdi.Flervalg
import no.nav.dagpenger.saksbehandling.klage.Verdi.TekstVerdi
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KlageBehandlingVedtakDistribuertTest {
    private val saksbehandler =
        Saksbehandler(
            navIdent = "saksbehandler",
            grupper = setOf("SaksbehandlerADGruppe"),
            tilganger = setOf(SAKSBEHANDLER),
        )

    @Test
    fun `vedtakDistribuert returnerer OversendKlageinstans når utfall er OPPRETTHOLDT`() {
        val klageBehandling = lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450")

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-123",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { "jp-original-vedtak" },
            )

        aksjon.shouldBeInstanceOf<OversendKlageinstans>()
        aksjon.behandlingId shouldBe klageBehandling.behandlingId
        aksjon.ident shouldBe "12345678901"
        aksjon.behandlendeEnhet shouldBe "4450"
    }

    @Test
    fun `vedtakDistribuert returnerer IngenAksjon når utfall er AVVIST`() {
        val klageBehandling = lagKlageBehandlingMedUtfall(UtfallType.AVVIST)

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-123",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        val aksjon =
            klageBehandling.vedtakDistribuert(
                hendelse = hendelse,
                fagsakId = "SAK123",
                fagsakId = "SAK123",
                finnJournalpostIdForBehandling = { null },
            )

        aksjon.shouldBeInstanceOf<IngenAksjon>()
        aksjon.behandlingId shouldBe klageBehandling.behandlingId
    }

    @Test
    fun `vedtakDistribuert returnerer IngenAksjon når utfall er MEDHOLD`() {
        val klageBehandling = lagKlageBehandlingMedUtfall(UtfallType.MEDHOLD)

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-123",
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
    fun `vedtakDistribuert inkluderer opprinnelig vedtak journalpostId når opprettholdt`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it
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

        aksjon.tilknyttedeJournalposter.any { it.journalpostId == "jp-original-vedtak" && it.type == "VEDTAK" } shouldBe true
    }

    @Test
    fun `vedtakDistribuert inkluderer klagemelding journalpostId`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450")

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

        aksjon.tilknyttedeJournalposter.any { it.journalpostId == "jp-vedtak-ny" && it.type == "KLAGEMELDING" } shouldBe true
    }

    @Test
    fun `vedtakDistribuert inkluderer fullmektig data når HVEM_KLAGER er FULLMEKTIG`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it
                it.svar(it.finnOpplysningId(HVEM_KLAGER), Flervalg(listOf(HvemKlagerType.FULLMEKTIG.name)))
                it.svar(it.finnOpplysningId(FULLMEKTIG_NAVN), TekstVerdi("Advokat Hansen"))
                it.svar(it.finnOpplysningId(FULLMEKTIG_ADRESSE_1), TekstVerdi("Storgata 1"))
                it.svar(it.finnOpplysningId(FULLMEKTIG_ADRESSE_2), TekstVerdi(""))
                it.svar(it.finnOpplysningId(FULLMEKTIG_ADRESSE_3), TekstVerdi(""))
                it.svar(it.finnOpplysningId(FULLMEKTIG_POSTNR), TekstVerdi("0001"))
                it.svar(it.finnOpplysningId(FULLMEKTIG_POSTSTED), TekstVerdi("Oslo"))
                it.svar(it.finnOpplysningId(FULLMEKTIG_LAND), TekstVerdi("Norge"))
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
        aksjon.fullmektigData["fullmektigAdresse1"] shouldBe "Storgata 1"
        aksjon.fullmektigData["fullmektigPostnr"] shouldBe "0001"
        aksjon.fullmektigData["fullmektigPoststed"] shouldBe "Oslo"
        aksjon.fullmektigData["fullmektigLand"] shouldBe "Norge"
    }

    @Test
    fun `vedtakDistribuert inkluderer ikke fullmektig data når HVEM_KLAGER er BRUKER`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it
                it.svar(it.finnOpplysningId(HVEM_KLAGER), Flervalg(listOf(HvemKlagerType.BRUKER.name)))
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

        aksjon.fullmektigData.isEmpty() shouldBe true
    }

    @Test
    fun `vedtakDistribuert inkluderer INTERN_MELDING som kommentar`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it
                it.svar(it.finnOpplysningId(INTERN_MELDING), TekstVerdi("Dette er en intern melding"))
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

        aksjon.kommentar shouldBe "Dette er en intern melding"
    }

    @Test
    fun `vedtakDistribuert kaster exception når behandlendeEnhet er null`() {
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
    fun `vedtakDistribuert endrer tilstand til OVERSEND_KLAGEINSTANS når OPPRETTHOLDT`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450")

        klageBehandling.tilstand().type shouldBe BEHANDLES

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                distribusjonId = "dist-123",
                journalpostId = "jp-vedtak-ny",
                utsendingId = UUID.randomUUID(),
                ident = "12345678901",
            )

        klageBehandling.vedtakDistribuert(
            hendelse = hendelse,
            fagsakId = "SAK123",
            finnJournalpostIdForBehandling = { null },
        )

        klageBehandling.tilstand().type shouldBe OVERSEND_KLAGEINSTANS
    }

    @Test
    fun `vedtakDistribuert inkluderer hjemler fra klagebehandling`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfallOgEnhet(UtfallType.OPPRETTHOLDT, "4450").also {
                it
                it.svar(it.finnOpplysningId(HJEMLER), Flervalg(listOf("§4-2", "§4-3")))
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

        aksjon.hjemler shouldBe listOf("§4-2", "§4-3")
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
