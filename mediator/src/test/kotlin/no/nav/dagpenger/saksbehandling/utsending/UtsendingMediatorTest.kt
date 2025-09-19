package no.nav.dagpenger.saksbehandling.utsending

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakPostgresRepository
import no.nav.dagpenger.saksbehandling.helper.arkiverbartDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.behandlingResultatEvent
import no.nav.dagpenger.saksbehandling.helper.distribuertDokumentBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.journalføringBehovLøsning
import no.nav.dagpenger.saksbehandling.helper.lagreOppgave
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.lagPerson
import no.nav.dagpenger.saksbehandling.mottak.ArenaSinkVedtakOpprettetMottak
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.toUrn
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerArkiverbarVersjonAvBrev
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerDistribuering
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.AvventerJournalføring
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.Distribuert
import no.nav.dagpenger.saksbehandling.utsending.Utsending.Tilstand.Type.VenterPåVedtak
import no.nav.dagpenger.saksbehandling.utsending.db.PostgresUtsendingRepository
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import no.nav.dagpenger.saksbehandling.utsending.mottak.BehandlingsResultatMottakForUtsending
import no.nav.dagpenger.saksbehandling.utsending.mottak.UtsendingBehovLøsningMottak
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Base64

class UtsendingMediatorTest {
    private val rapid = TestRapid()

    @Test
    fun `livssyklus for en utsending uten brev ved opprettelse når vedtak fattes i dp-sak`() {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                utløstAv = UtløstAvType.SØKNAD,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )
        val person = lagPerson()

        DBTestHelper.withBehandling(behandling = behandling, person = person) { ds ->
            val oppgave =
                lagreOppgave(dataSource = ds, behandlingId = behandling.behandlingId, personIdent = person.ident)
            val behandlingId = oppgave.behandlingId
            val søknadId = UUIDv7.ny()
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

            BehandlingsResultatMottakForUtsending(
                rapidsConnection = rapid,
                utsendingMediator = utsendingMediator,
                sakRepository = SakPostgresRepository(ds),
            )

            UtsendingBehovLøsningMottak(
                utsendingMediator = utsendingMediator,
                rapidsConnection = rapid,
            )

            utsendingMediator.opprettUtsending(
                behandlingId = oppgave.behandlingId,
                brev = null,
                ident = oppgave.personIdent(),
                type = UtsendingType.KLAGEMELDING,
            )

            var utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.behandlingId shouldBe behandlingId
            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe null

            val message =
                behandlingResultatEvent(
                    ident = person.ident,
                    behandlingId = behandling.behandlingId.toString(),
                    søknadId = søknadId.toString(),
                    behandletHendelseType = "Søknad",
                    harRett = true,
                )

            rapid.sendTestMessage(message = message)

            utsendingRepository.finnUtsendingForBehandlingId(behandlingId).let { utsending ->
                requireNotNull(utsending)
                utsending.brev() shouldBe htmlBrev
            }

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)

            utsending.sak() shouldBe utsendingSak
            utsending.tilstand().type shouldBe AvventerArkiverbarVersjonAvBrev

            rapid.inspektør.size shouldBe 2
            val htmlBrevAsBase64 = Base64.getEncoder().encode(htmlBrev.toByteArray()).toString(Charsets.UTF_8)
            rapid.inspektør.message(0).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                   "@event_name": "behov",
                   "@behov": [
                     "${ArkiverbartBrevBehov.BEHOV_NAVN}"
                   ],
                   "htmlBase64": "$htmlBrevAsBase64",
                   "dokumentNavn": "vedtak.pdf",
                   "kontekst": "behandling/$behandlingId",
                   "ident": "${oppgave.personIdent()}",
                   "sak": {
                      "id": "${utsendingSak.id}",
                      "kontekst": "${utsendingSak.kontekst}"
                  }
                }
                """.trimIndent()

            rapid.inspektør.message(1).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                   "@event_name": "vedtak_fattet_utenfor_arena",
                    "behandlingId" : "$behandlingId",
                    "søknadId" : "$søknadId",
                    "ident" : "${person.ident}",
                    "sakId" : "$sakId"
                }
                """.trimIndent()

            val pdfUrnString = "urn:pdf:123"
            rapid.sendTestMessage(
                arkiverbartDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    pdfUrnString = pdfUrnString,
                ),
            )

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe AvventerJournalføring
            utsending.pdfUrn() shouldBe pdfUrnString.toUrn()
            rapid.inspektør.size shouldBe 3
            rapid.inspektør.message(2).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${JournalføringBehov.BEHOV_NAVN}"
                  ],
                  "tittel" : "${UtsendingType.KLAGEMELDING.brevTittel}",
                  "skjemaKode" : "${UtsendingType.KLAGEMELDING.skjemaKode}",
                  "ident": "${oppgave.personIdent()}",
                  "pdfUrn": "$pdfUrnString",
                  "sak": {
                    "id": "${utsendingSak.id}",
                    "kontekst": "${utsendingSak.kontekst}"
                  }
                }
                """.trimIndent()

            val journalpostId = "123"
            rapid.sendTestMessage(journalføringBehovLøsning(behandlingId = behandlingId, journalpostId = journalpostId))

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe AvventerDistribuering
            utsending.journalpostId() shouldBe journalpostId
            utsending.sak() shouldBe utsendingSak
            rapid.inspektør.size shouldBe 4
            rapid.inspektør.message(3).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${DistribueringBehov.BEHOV_NAVN}"
                  ],
                  "journalpostId": "${utsending.journalpostId()}",
                  "fagsystem": "${utsendingSak.kontekst}"
                }
                """.trimIndent()

            val distribusjonId = "distribusjonId"
            rapid.sendTestMessage(
                distribuertDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    journalpostId = journalpostId,
                    distribusjonId = distribusjonId,
                ),
            )
            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe Distribuert
            utsending.distribusjonId() shouldBe distribusjonId
        }
    }

    @Test
    fun `livssyklus for en utsending uten brev ved opprettelse når vedtak fattes i Arena`() {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                utløstAv = UtløstAvType.SØKNAD,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )
        val person = lagPerson()

        DBTestHelper.withBehandling(behandling = behandling, person = person) { ds ->
            val oppgave =
                lagreOppgave(dataSource = ds, behandlingId = behandling.behandlingId, personIdent = person.ident)
            val behandlingId = oppgave.behandlingId
            val utsendingSak = UtsendingSak("123", "Arena")
            val htmlBrev = "<H1>Hei</H1><p>Her er et brev</p>"

            val utsendingRepository = PostgresUtsendingRepository(ds)
            val utsendingMediator =
                UtsendingMediator(
                    utsendingRepository = utsendingRepository,
                    brevProdusent =
                        mockk<UtsendingMediator.BrevProdusent>().also {
                            coEvery { it.lagBrev(person.ident, behandlingId, utsendingSak.id) } returns htmlBrev
                        },
                ).also {
                    it.setRapidsConnection(rapid)
                }

            val mockSakMediator =
                mockk<SakMediator>().also {
                    every { it.oppdaterSakMedArenaSakId(any()) } just Runs
                }
            ArenaSinkVedtakOpprettetMottak(
                rapidsConnection = rapid,
                utsendingMediator = utsendingMediator,
                personRepository = PostgresPersonRepository(ds),
                sakMediator = mockSakMediator,
            )

            UtsendingBehovLøsningMottak(
                utsendingMediator = utsendingMediator,
                rapidsConnection = rapid,
            )

            utsendingMediator.opprettUtsending(
                behandlingId = behandlingId,
                brev = null,
                ident = oppgave.personIdent(),
                type = UtsendingType.KLAGEMELDING,
            )

            var utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.behandlingId shouldBe behandlingId
            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe null

            //language=JSON
            rapid.sendTestMessage(
                message =
                    """
                    {
                      "@event_name": "arenasink_vedtak_opprettet",
                      "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
                      "sakId": ${utsendingSak.id},
                      "rettighet": "Dagpenger",
                      "vedtakId": 0,
                      "vedtakstatus": "IVERK",
                      "utfall": false,
                      "kilde": {
                        "id": "$behandlingId",
                        "system": "dp-behandling"
                      }
                    }
                              
                    """.trimIndent(),
            )

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.sak() shouldBe utsendingSak
            utsending.tilstand().type shouldBe AvventerArkiverbarVersjonAvBrev

            rapid.inspektør.size shouldBe 1
            val htmlBrevAsBase64 = Base64.getEncoder().encode(htmlBrev.toByteArray()).toString(Charsets.UTF_8)
            rapid.inspektør.message(0).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                   "@event_name": "behov",
                   "@behov": [
                     "${ArkiverbartBrevBehov.BEHOV_NAVN}"
                   ],
                   "htmlBase64": "$htmlBrevAsBase64",
                   "dokumentNavn": "vedtak.pdf",
                   "kontekst": "behandling/$behandlingId",
                   "ident": "${oppgave.personIdent()}",
                   "sak": {
                      "id": "${utsendingSak.id}",
                      "kontekst": "${utsendingSak.kontekst}"
                  }
                }
                """.trimIndent()

            val pdfUrnString = "urn:pdf:123"
            rapid.sendTestMessage(
                arkiverbartDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    pdfUrnString = pdfUrnString,
                ),
            )

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe AvventerJournalføring
            utsending.pdfUrn() shouldBe pdfUrnString.toUrn()
            rapid.inspektør.size shouldBe 2
            rapid.inspektør.message(1).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${JournalføringBehov.BEHOV_NAVN}"
                  ],
                  "tittel" : "${UtsendingType.KLAGEMELDING.brevTittel}",
                  "skjemaKode" : "${UtsendingType.KLAGEMELDING.skjemaKode}",
                  "ident": "${oppgave.personIdent()}",
                  "pdfUrn": "$pdfUrnString",
                  "sak": {
                    "id": "${utsendingSak.id}",
                    "kontekst": "${utsendingSak.kontekst}"
                  }
                }
                """.trimIndent()

            val journalpostId = "123"
            rapid.sendTestMessage(journalføringBehovLøsning(behandlingId = behandlingId, journalpostId = journalpostId))

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe AvventerDistribuering
            utsending.journalpostId() shouldBe journalpostId
            rapid.inspektør.size shouldBe 3
            rapid.inspektør.message(2).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${DistribueringBehov.BEHOV_NAVN}"
                  ],
                  "journalpostId": "${utsending.journalpostId()}",
                  "fagsystem": "${utsendingSak.kontekst}"
                }
                """.trimIndent()

            val distribusjonId = "distribusjonId"
            rapid.sendTestMessage(
                distribuertDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    journalpostId = journalpostId,
                    distribusjonId = distribusjonId,
                ),
            )
            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe Distribuert
            utsending.distribusjonId() shouldBe distribusjonId
        }
    }

    @Test
    fun `livssyklus for en utsending med brev ved opprettelse`() {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                utløstAv = UtløstAvType.KLAGE,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )
        val person = lagPerson()

        DBTestHelper.withBehandling(behandling = behandling, person = person) { ds ->
            val oppgave =
                lagreOppgave(dataSource = ds, behandlingId = behandling.behandlingId, personIdent = person.ident)
            val behandlingId = oppgave.behandlingId
            val utsendingRepository = PostgresUtsendingRepository(ds)
            val utsendingMediator =
                UtsendingMediator(
                    utsendingRepository = utsendingRepository,
                    brevProdusent = mockk(),
                ).also {
                    it.setRapidsConnection(rapid)
                }

            UtsendingBehovLøsningMottak(
                utsendingMediator = utsendingMediator,
                rapidsConnection = rapid,
            )

            val htmlBrev = "<H1>Hei</H1><p>Her er et brev</p>"
            utsendingMediator.opprettUtsending(
                behandlingId = behandlingId,
                brev = htmlBrev,
                ident = oppgave.personIdent(),
                type = UtsendingType.KLAGEMELDING,
            )

            var utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.behandlingId shouldBe behandlingId
            utsending.tilstand().type shouldBe VenterPåVedtak
            utsending.brev() shouldBe htmlBrev

            val utsendingSak = UtsendingSak("sakId", "fagsystem")

            utsendingMediator.mottaStartUtsending(
                startUtsendingHendelse =
                    StartUtsendingHendelse(
                        utsendingSak = utsendingSak,
                        behandlingId = behandlingId,
                        ident = oppgave.personIdent(),
                        brev = null,
                    ),
            )

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.sak() shouldBe utsendingSak
            utsending.tilstand().type shouldBe AvventerArkiverbarVersjonAvBrev

            rapid.inspektør.size shouldBe 1
            val htmlBrevAsBase64 = Base64.getEncoder().encode(htmlBrev.toByteArray()).toString(Charsets.UTF_8)
            rapid.inspektør.message(0).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                   "@event_name": "behov",
                   "@behov": [
                     "${ArkiverbartBrevBehov.BEHOV_NAVN}"
                   ],
                   "htmlBase64": "$htmlBrevAsBase64",
                   "dokumentNavn": "vedtak.pdf",
                   "kontekst": "behandling/$behandlingId",
                   "ident": "${oppgave.personIdent()}",
                   "sak": {
                      "id": "${utsendingSak.id}",
                      "kontekst": "${utsendingSak.kontekst}"
                  }
                }
                """.trimIndent()

            val pdfUrnString = "urn:pdf:123"
            rapid.sendTestMessage(
                arkiverbartDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    pdfUrnString = pdfUrnString,
                ),
            )

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe AvventerJournalføring
            utsending.pdfUrn() shouldBe pdfUrnString.toUrn()
            rapid.inspektør.size shouldBe 2
            rapid.inspektør.message(1).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${JournalføringBehov.BEHOV_NAVN}"
                  ],
                  "tittel" : "${UtsendingType.KLAGEMELDING.brevTittel}",
                  "skjemaKode" : "${UtsendingType.KLAGEMELDING.skjemaKode}",
                  "ident": "${oppgave.personIdent()}",
                  "pdfUrn": "$pdfUrnString",
                  "sak": {
                    "id": "${utsendingSak.id}",
                    "kontekst": "${utsendingSak.kontekst}"
                  }
                }
                """.trimIndent()

            val journalpostId = "123"
            rapid.sendTestMessage(journalføringBehovLøsning(behandlingId = behandlingId, journalpostId = journalpostId))

            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe AvventerDistribuering
            utsending.journalpostId() shouldBe journalpostId
            rapid.inspektør.size shouldBe 3
            rapid.inspektør.message(2).toString() shouldEqualSpecifiedJson
                //language=JSON
                """
                {
                  "@event_name": "behov",
                  "@behov": [
                    "${DistribueringBehov.BEHOV_NAVN}"
                  ],
                  "journalpostId": "${utsending.journalpostId()}"
                }
                """.trimIndent()

            val distribusjonId = "distribusjonId"
            rapid.sendTestMessage(
                distribuertDokumentBehovLøsning(
                    behandlingId = behandlingId,
                    journalpostId = journalpostId,
                    distribusjonId = distribusjonId,
                ),
            )
            utsending = utsendingRepository.hentUtsendingForBehandlingId(behandlingId)
            utsending.tilstand().type shouldBe Distribuert
            utsending.distribusjonId() shouldBe distribusjonId
        }
    }
}
