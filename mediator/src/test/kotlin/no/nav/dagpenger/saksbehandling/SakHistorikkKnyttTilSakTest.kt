package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SakHistorikkKnyttTilSakTest {
    private val now = LocalDateTime.now()
    private val testPerson =
        Person(
            id = UUIDv7.ny(),
            ident = "12345678910",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )

    private val behandling =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = UtløstAvType.SØKNAD,
            opprettet = now,
            hendelse = TomHendelse,
        )
    private val sakMedBehandling =
        Sak(søknadId = UUIDv7.ny(), opprettet = now).also {
            it.leggTilBehandling(
                behandling,
            )
        }
    private val sakUtenBehandling = Sak(søknadId = UUIDv7.ny(), opprettet = now)

    private val sakHistorikk =
        SakHistorikk(
            person = testPerson,
        ).also {
            it.leggTilSak(sakMedBehandling)
            it.leggTilSak(sakUtenBehandling)
        }

    @Test
    fun `knyttTilSak med SøknadsbehandlingOpprettetHendelse`() {
        val hendelse =
            SøknadsbehandlingOpprettetHendelse(
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = null,
            )

        sakHistorikk.knyttTilSak(hendelse) shouldBe
            KnyttTilSakResultat.IkkeKnyttetTilSak(
                sakMedBehandling.sakId,
                sakUtenBehandling.sakId,
            )

        sakHistorikk.knyttTilSak(
            hendelse.copy(
                basertPåBehandling = behandling.behandlingId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sakMedBehandling)

        val sakMedSammeBehandling = sakMedBehandling.copy(sakId = UUIDv7.ny())
        sakHistorikk.leggTilSak(sakMedSammeBehandling)
        sakHistorikk.knyttTilSak(
            hendelse.copy(
                basertPåBehandling = behandling.behandlingId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilFlereSaker(sakMedBehandling.sakId, sakMedSammeBehandling.sakId)
    }

    @Test
    fun `knyttTilSak med MeldekortbehandlingOpprettetHendelse`() {
        val hendelse =
            MeldekortbehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                meldekortId = "id",
                basertPåBehandling = UUIDv7.ny(),
            )

        sakHistorikk.knyttTilSak(hendelse) shouldBe
            KnyttTilSakResultat.IkkeKnyttetTilSak(
                sakMedBehandling.sakId,
                sakUtenBehandling.sakId,
            )

        sakHistorikk.knyttTilSak(
            hendelse.copy(
                basertPåBehandling = behandling.behandlingId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sakMedBehandling)

        val sakMedSammeBehandling = sakMedBehandling.copy(sakId = UUIDv7.ny())
        sakHistorikk.leggTilSak(sakMedSammeBehandling)
        sakHistorikk.knyttTilSak(
            hendelse.copy(
                basertPåBehandling = behandling.behandlingId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilFlereSaker(sakMedBehandling.sakId, sakMedSammeBehandling.sakId)
    }

    @Test
    fun `knyttTilSak med ManuellBehandlingOpprettetHendelse`() {
        val hendelse =
            ManuellBehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                manuellId = UUIDv7.ny(),
                basertPåBehandling = UUIDv7.ny(),
            )

        sakHistorikk.knyttTilSak(hendelse) shouldBe
            KnyttTilSakResultat.IkkeKnyttetTilSak(
                sakMedBehandling.sakId,
                sakUtenBehandling.sakId,
            )

        sakHistorikk.knyttTilSak(
            hendelse.copy(
                basertPåBehandling = behandling.behandlingId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sakMedBehandling)

        val sakMedSammeBehandling = sakMedBehandling.copy(sakId = UUIDv7.ny())
        sakHistorikk.leggTilSak(sakMedSammeBehandling)
        sakHistorikk.knyttTilSak(
            hendelse.copy(
                basertPåBehandling = behandling.behandlingId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilFlereSaker(sakMedBehandling.sakId, sakMedSammeBehandling.sakId)
    }

    @Test
    fun `knyttTilSak med BehandlingOpprettetHendelse`() {
        val hendelse =
            BehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                sakId = UUIDv7.ny(),
                type = UtløstAvType.SØKNAD,
            )

        sakHistorikk.knyttTilSak(hendelse) shouldBe
            KnyttTilSakResultat.IkkeKnyttetTilSak(
                sakMedBehandling.sakId,
                sakUtenBehandling.sakId,
            )

        sakHistorikk.knyttTilSak(
            hendelse.copy(
                sakId = sakMedBehandling.sakId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sakMedBehandling)

        val sakMedSammeSakId = sakUtenBehandling.copy(sakId = sakMedBehandling.sakId)
        sakHistorikk.leggTilSak(sakMedSammeSakId)
        sakHistorikk.knyttTilSak(
            hendelse.copy(
                sakId = sakMedBehandling.sakId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilFlereSaker(sakMedBehandling.sakId, sakMedSammeSakId.sakId)
    }
}
