package no.nav.dagpenger.saksbehandling.oppfolging

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.OpprettBehandlingTypeDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageinstansVedtakHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageAksjon
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.modell.helpers.TestHelpers.Klage.lagKlageBehandlingMedUtfall
import no.nav.dagpenger.saksbehandling.modell.helpers.TestHelpers.Person.lagPerson
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class OppfølgingBehandlerTest {
    private val saksbehandler = Saksbehandler(navIdent = "saksbehandler1", emptySet())

    @Test
    fun `opprettBehandling med OpprettRevurderingBehandlingEtterKlage slår opp kabalReferanse fra klagebehandlingen`() {
        val klageBehandling =
            lagKlageBehandlingMedUtfall(
                tilstand = KlageBehandling.BehandlesAvKlageinstans,
                utfallType = UtfallType.OPPRETTHOLDELSE,
            )
        val kabalReferanse = UUID.randomUUID()
        val aksjon =
            klageBehandling.mottaKlageinstansVedtak(
                KlageinstansVedtakHendelse(
                    type = KlageinstansVedtakHendelse.KlageinstansVedtakType.KLAGE,
                    klageId = klageBehandling.behandlingId,
                    klageinstansVedtakId = kabalReferanse,
                    avsluttet = LocalDateTime.now(),
                    utfall = "MEDHOLD",
                    journalpostIder = listOf("journalpostId1"),
                ),
            )
        aksjon.shouldBeInstanceOf<KlageAksjon.StartRevurdering>()

        val klageMediatorMock =
            mockk<KlageMediator>().also {
                every { it.hentKlageBehandlingUtenTilgangssjekk(klageBehandling.behandlingId) } returns klageBehandling
            }

        val opprettetBehandlingId = UUID.randomUUID()
        val opprettBehandlingTypeDTOSlot = slot<OpprettBehandlingTypeDTO>()
        val behandlingKlientMock =
            mockk<BehandlingKlient>().also {
                every {
                    it.opprettBehandling(capture(opprettBehandlingTypeDTOSlot), any())
                } returns Result.success(opprettetBehandlingId)
            }

        val oppfølgingBehandler = OppfølgingBehandler(klageMediatorMock, behandlingKlientMock)

        val oppfølging =
            Oppfølging.opprett(
                person = lagPerson(),
                tittel = "Vurder revurdering etter klageinstansvedtak",
                strukturertData =
                    mapOf(
                        "kabalReferanse" to kabalReferanse.toString(),
                        "kabalUtfall" to "MEDHOLD",
                        "basertPåBehandling" to klageBehandling.behandlingId.toString(),
                    ),
            )

        val valgtSakId = UUID.randomUUID()
        oppfølging.startFerdigstilling(vurdering = "Skal revurderes", aksjon = OppfølgingAksjon.Avslutt(valgtSakId))
        val ferdigstiltHendelse =
            oppfølgingBehandler.opprettBehandling(
                oppfølging = oppfølging,
                hendelse =
                    FerdigstillOppfølgingHendelse(
                        oppfølgingId = oppfølging.id,
                        aksjon =
                            OppfølgingAksjon.OpprettRevurderingBehandlingEtterKlage(
                                saksbehandlerToken = "token",
                                valgtSakId = valgtSakId,
                            ),
                        vurdering = "Skal revurderes",
                        utførtAv = saksbehandler,
                    ),
            )

        ferdigstiltHendelse.aksjonType shouldBe OppfølgingAksjon.Type.OPPRETT_REVURDERING_BEHANDLING_ETTER_KLAGE
        ferdigstiltHendelse.opprettetBehandlingId shouldBe opprettetBehandlingId

        verify(exactly = 1) {
            behandlingKlientMock.opprettBehandling(any(), "token")
        }
        with(opprettBehandlingTypeDTOSlot.captured) {
            shouldBeInstanceOf<OpprettBehandlingTypeDTO.RevurderingEtterKlage>()
            personIdent shouldBe oppfølging.person.ident
            hendelseDato shouldBe oppfølging.opprettet.toLocalDate()
            hendelseId shouldBe oppfølging.id.toString()
            this.kabalReferanse shouldBe kabalReferanse
            begrunnelse shouldBe "Skal revurderes"
        }
    }

    @Test
    fun `opprettBehandling med OpprettRevurderingBehandlingEtterKlage kaster exception hvis basertPåBehandling mangler`() {
        val klageMediatorMock = mockk<KlageMediator>()
        val behandlingKlientMock = mockk<BehandlingKlient>()
        val oppfølgingBehandler = OppfølgingBehandler(klageMediatorMock, behandlingKlientMock)

        val oppfølging =
            Oppfølging.opprett(
                person = lagPerson(),
                tittel = "Vurder revurdering etter klageinstansvedtak",
            )

        val hendelse =
            FerdigstillOppfølgingHendelse(
                oppfølgingId = oppfølging.id,
                aksjon =
                    OppfølgingAksjon.OpprettRevurderingBehandlingEtterKlage(
                        saksbehandlerToken = "token",
                        valgtSakId = UUID.randomUUID(),
                    ),
                vurdering = null,
                utførtAv = saksbehandler,
            )

        runCatching {
            oppfølgingBehandler.opprettBehandling(oppfølging, hendelse)
        }.isFailure shouldBe true
    }
}
