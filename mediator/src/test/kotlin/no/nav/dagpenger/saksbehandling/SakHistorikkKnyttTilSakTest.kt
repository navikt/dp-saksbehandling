package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.DpBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDateTime
import java.util.stream.Stream

class SakHistorikkKnyttTilSakTest {
    companion object {
        @JvmStatic
        fun dpBehandlingTyper(): Stream<Arguments> =
            Stream.of(
                Arguments.of(HendelseBehandler.DpBehandling.Meldekort, "id"),
                Arguments.of(HendelseBehandler.DpBehandling.Manuell, UUIDv7.ny().toString()),
                Arguments.of(HendelseBehandler.DpBehandling.Ferietillegg, "ferie-1"),
            )
    }

    private val now = LocalDateTime.now()
    private val testPerson =
        Person(
            id = UUIDv7.ny(),
            ident = "12345678910",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )

    private val søknadOmNyRettBehandling =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = HendelseBehandler.DpBehandling.Søknad,
            opprettet = now,
            hendelse = TomHendelse,
        )
    private val sakMedBehandling =
        Sak(opprettet = now).also {
            it.leggTilBehandling(
                søknadOmNyRettBehandling,
            )
        }
    private val sakUtenBehandling = Sak(opprettet = now)

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
                behandlingskjedeId = UUIDv7.ny(),
            )

        sakHistorikk.knyttTilSak(hendelse) shouldBe
            KnyttTilSakResultat.IkkeKnyttetTilSak(
                sakMedBehandling.sakId,
                sakUtenBehandling.sakId,
            )

        sakHistorikk.knyttTilSak(
            hendelse.copy(
                basertPåBehandling = søknadOmNyRettBehandling.behandlingId,
                behandlingskjedeId = sakMedBehandling.sakId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sakMedBehandling)
    }

    @ParameterizedTest
    @MethodSource("dpBehandlingTyper")
    fun `knyttTilSak med DpBehandlingOpprettetHendelse`(
        type: HendelseBehandler.DpBehandling,
        eksternId: String,
    ) {
        val hendelse =
            DpBehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                behandlingskjedeId = UUIDv7.ny(),
                type = type,
                eksternId = eksternId,
            )

        sakHistorikk.knyttTilSak(hendelse) shouldBe
            KnyttTilSakResultat.IkkeKnyttetTilSak(
                sakMedBehandling.sakId,
                sakUtenBehandling.sakId,
            )

        sakHistorikk.knyttTilSak(
            hendelse.copy(
                basertPåBehandling = UUIDv7.ny(),
                behandlingskjedeId = sakMedBehandling.sakId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sakMedBehandling)

        sakHistorikk.knyttTilSak(
            hendelse.copy(
                behandlingskjedeId = UUIDv7.ny(),
                basertPåBehandling = søknadOmNyRettBehandling.behandlingId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sakMedBehandling)
    }

    @Test
    fun `knyttTilSak med BehandlingOpprettetHendelse`() {
        val hendelse =
            BehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                sakId = UUIDv7.ny(),
                type = HendelseBehandler.DpBehandling.Søknad,
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
