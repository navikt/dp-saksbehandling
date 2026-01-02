package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldEqualSpecifiedJsonIgnoringOrder
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert
import no.nav.dagpenger.saksbehandling.klage.HvemKlagerType
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.modell.helpers.TestHelpers
import org.junit.jupiter.api.Test
import java.util.UUID

class KlageInstansBehovByggerTest {
    private val sakId = UUID.randomUUID()
    private val utsendingId = UUID.randomUUID()
    private val journalpostIdForOpprinneligVedtak = "originalJournalpostId"

    @Test
    fun `bygger behov for klageinstans med minimal data`() {
        val klageBehandling =
            TestHelpers.Klage.lagKlageBehandling(
                journalpostId = "klageJournalpostId",
                behandlendeEnhet = "4450",
            )

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = utsendingId,
                ident = TestHelpers.Person.personIdent,
                journalpostId = "vedtakJournalpostId",
                distribusjonId = UUID.randomUUID().toString(),
            )

        val bygger =
            KlageInstansBehovBygger(
                klageBehandling = klageBehandling,
                sakId = sakId,
                hendelse = hendelse,
                finnJournalpostIdForBehandling = { null },
            )

        val behov = bygger.behov

        behov shouldEqualSpecifiedJsonIgnoringOrder """
            {
                "@event_name": "behov",
                "@behov": ["OversendelseKlageinstans"],
                "behandlingId": "${klageBehandling.behandlingId}",
                "ident": "${TestHelpers.Person.personIdent}",
                "fagsakId": "$sakId",
                "behandlendeEnhet": "4450",
                "tilknyttedeJournalposter": [
                    {
                        "type": "KLAGE_VEDTAK",
                        "journalpostId": "vedtakJournalpostId"
                    },
                    {
                        "type": "BRUKERS_KLAGE",
                        "journalpostId": "klageJournalpostId"
                    }
                ]
            }
        """
    }

    @Test
    fun `bygger behov med alle tilknyttede journalposter`() {
        val klageBehandling =
            TestHelpers.Klage.lagKlageBehandling(
                journalpostId = "klageJournalpostId",
            )

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = utsendingId,
                ident = TestHelpers.Person.personIdent,
                journalpostId = "vedtakJournalpostId",
                distribusjonId = UUID.randomUUID().toString(),
            )

        val bygger =
            KlageInstansBehovBygger(
                klageBehandling = klageBehandling,
                sakId = sakId,
                hendelse = hendelse,
                finnJournalpostIdForBehandling = { journalpostIdForOpprinneligVedtak },
            )

        val behov = bygger.behov
        behov shouldEqualSpecifiedJsonIgnoringOrder """
            {
                "@event_name": "behov",
                "@behov": ["OversendelseKlageinstans"],
                "behandlingId": "${klageBehandling.behandlingId}",
                "ident": "${TestHelpers.Person.personIdent}",
                "fagsakId": "$sakId",
                "behandlendeEnhet": "4449",
                "tilknyttedeJournalposter": [
                    {
                        "type": "KLAGE_VEDTAK",
                        "journalpostId": "vedtakJournalpostId"
                    },
                    {
                        "type": "BRUKERS_KLAGE",
                        "journalpostId": "klageJournalpostId"
                    },
                    {
                        "type": "OPPRINNELIG_VEDTAK",
                        "journalpostId": "$journalpostIdForOpprinneligVedtak"
                    }
                ]
            }
        """
    }

    @Test
    fun `bygger behov med fullmektig data`() {
        val klageBehandling =
            TestHelpers.Klage.lagKlageBehandling(
                opplysninger =
                    setOf(
                        Opplysning(
                            type = OpplysningType.HVEM_KLAGER,
                            verdi = Verdi.TekstVerdi(HvemKlagerType.FULLMEKTIG.name),
                        ),
                        Opplysning(
                            type = OpplysningType.FULLMEKTIG_NAVN,
                            verdi = Verdi.TekstVerdi("Advokat Hansen"),
                        ),
                        Opplysning(
                            type = OpplysningType.FULLMEKTIG_ADRESSE_1,
                            verdi = Verdi.TekstVerdi("Advokateveien 1"),
                        ),
                        Opplysning(
                            type = OpplysningType.FULLMEKTIG_ADRESSE_2,
                            verdi = Verdi.TekstVerdi("Etasje 2"),
                        ),
                        Opplysning(
                            type = OpplysningType.FULLMEKTIG_ADRESSE_3,
                            verdi = Verdi.TekstVerdi("Inngang 3"),
                        ),
                        Opplysning(
                            type = OpplysningType.FULLMEKTIG_POSTNR,
                            verdi = Verdi.TekstVerdi("0123"),
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

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = utsendingId,
                ident = TestHelpers.Person.personIdent,
                journalpostId = "vedtakJournalpostId",
                distribusjonId = UUID.randomUUID().toString(),
            )

        val bygger =
            KlageInstansBehovBygger(
                klageBehandling = klageBehandling,
                sakId = sakId,
                hendelse = hendelse,
                finnJournalpostIdForBehandling = { null },
            )

        val behov = bygger.behov
        behov shouldEqualSpecifiedJsonIgnoringOrder """
            {
                "@behov": ["OversendelseKlageinstans"],
                "behandlingId": "${klageBehandling.behandlingId}",
                "ident": "${TestHelpers.Person.personIdent}",
                "fagsakId": "$sakId",
                "behandlendeEnhet": "4449",
                "tilknyttedeJournalposter": [
                    {
                        "type": "KLAGE_VEDTAK",
                        "journalpostId": "vedtakJournalpostId"
                    }
                ],
                "prosessfullmektigNavn": "Advokat Hansen",
                "prosessfullmektigAdresselinje1": "Advokateveien 1",
                "prosessfullmektigAdresselinje2": "Etasje 2",
                "prosessfullmektigAdresselinje3": "Inngang 3",
                "prosessfullmektigPostnummer": "0123",
                "prosessfullmektigPoststed": "Oslo",
                "prosessfullmektigLand": "NO"
            }
        """
    }

    @Test
    fun `bruker default behandlende enhet 4449 når enhet ikke er satt`() {
        val klageBehandling =
            TestHelpers.Klage.lagKlageBehandling(
                behandlendeEnhet = null,
            )

        val hendelse =
            UtsendingDistribuert(
                behandlingId = klageBehandling.behandlingId,
                utsendingId = utsendingId,
                ident = TestHelpers.Person.personIdent,
                journalpostId = "vedtakJournalpostId",
                distribusjonId = UUID.randomUUID().toString(),
            )

        val bygger =
            KlageInstansBehovBygger(
                klageBehandling = klageBehandling,
                sakId = sakId,
                hendelse = hendelse,
                finnJournalpostIdForBehandling = { null },
            )

        bygger.behov shouldContainJsonKey "behandlendeEnhet"
        bygger.behov shouldEqualSpecifiedJsonIgnoringOrder """
            {
                "@event_name": "behov",
                "@behov": ["OversendelseKlageinstans"],
                "behandlingId": "${klageBehandling.behandlingId}",
                "ident": "${TestHelpers.Person.personIdent}",
                "fagsakId": "$sakId",
                "behandlendeEnhet": "4449",
                "tilknyttedeJournalposter": [
                    {
                        "type": "KLAGE_VEDTAK",
                        "journalpostId": "vedtakJournalpostId"
                    }
                ]
            }
        """
    }
}
