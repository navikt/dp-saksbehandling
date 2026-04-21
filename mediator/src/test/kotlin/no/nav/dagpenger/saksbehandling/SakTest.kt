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

class SakTest {
    companion object {
        @JvmStatic
        fun dpBehandlingTyper(): Stream<Arguments> =
            Stream.of(
                Arguments.of(HendelseBehandler.DpBehandling.Meldekort, "id-123"),
                Arguments.of(HendelseBehandler.DpBehandling.Manuell, UUIDv7.ny().toString()),
                Arguments.of(HendelseBehandler.DpBehandling.Ferietillegg, "ferie-1"),
            )
    }

    private val now = LocalDateTime.now()
    private val behandling =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = HendelseBehandler.DpBehandling.Søknad,
            opprettet = now,
            hendelse = TomHendelse,
        )

    @Test
    fun `knyttTilSak med SøknadsbehandlingOpprettetHendelse`() {
        val behandlingskjedeId = UUIDv7.ny()
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now, sakId = UUIDv7.ny())
        sak.knyttTilSak(
            SøknadsbehandlingOpprettetHendelse(
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = null,
                behandlingskjedeId = behandlingskjedeId,
            ),
        ) shouldBe KnyttTilSakResultat.IkkeKnyttetTilSak(sak.sakId)

        sak.leggTilBehandling(behandling)
        sak.knyttTilSak(
            SøknadsbehandlingOpprettetHendelse(
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = behandling.behandlingId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)

        val sak2 = Sak(søknadId = UUIDv7.ny(), opprettet = now, sakId = behandlingskjedeId)
        sak2.knyttTilSak(
            søknadsbehandlingOpprettetHendelse =
                SøknadsbehandlingOpprettetHendelse(
                    søknadId = UUIDv7.ny(),
                    behandlingId = UUIDv7.ny(),
                    ident = "12345678910",
                    opprettet = now,
                    basertPåBehandling = null,
                    behandlingskjedeId = behandlingskjedeId,
                ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak2)
    }

    @ParameterizedTest
    @MethodSource("dpBehandlingTyper")
    fun `knyttTilSak med DpBehandlingOpprettetHendelse`(
        type: HendelseBehandler.DpBehandling,
        eksternId: String,
    ) {
        val behandlingskjedeId = UUIDv7.ny()
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now)

        sak.knyttTilSak(
            DpBehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                behandlingskjedeId = UUIDv7.ny(),
                type = type,
                eksternId = eksternId,
            ),
        ) shouldBe KnyttTilSakResultat.IkkeKnyttetTilSak(sak.sakId)

        sak.leggTilBehandling(behandling)
        sak.knyttTilSak(
            DpBehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = behandling.behandlingId,
                behandlingskjedeId = UUIDv7.ny(),
                type = type,
                eksternId = eksternId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)

        val sak2 = Sak(søknadId = UUIDv7.ny(), opprettet = now, sakId = behandlingskjedeId)
        sak2.knyttTilSak(
            DpBehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                behandlingskjedeId = behandlingskjedeId,
                type = type,
                eksternId = eksternId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak2)
    }

    @Test
    fun `knyttTilSak med BehandlingOpprettetHendelse`() {
        val sakId = UUIDv7.ny()
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now, sakId = sakId)

        sak.knyttTilSak(
            BehandlingOpprettetHendelse(
                sakId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                type = HendelseBehandler.DpBehandling.Søknad,
            ),
        ) shouldBe KnyttTilSakResultat.IkkeKnyttetTilSak(sak.sakId)

        sak.knyttTilSak(
            BehandlingOpprettetHendelse(
                sakId = sakId,
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                type = HendelseBehandler.DpBehandling.Søknad,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)
    }
}
