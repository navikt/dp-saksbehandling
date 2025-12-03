package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SakTest {
    private val now = LocalDateTime.now()
    private val behandling =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = UtløstAvType.SØKNAD,
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

    @Test
    fun `knyttTilSak med MeldekortbehandlingOpprettetHendelse`() {
        val behandlingskjedeId = UUIDv7.ny()
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now)

        sak.knyttTilSak(
            MeldekortbehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                meldekortId = "id",
            ),
        ) shouldBe KnyttTilSakResultat.IkkeKnyttetTilSak(sak.sakId)

        sak.leggTilBehandling(behandling)
        sak.knyttTilSak(
            MeldekortbehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = behandling.behandlingId,
                meldekortId = "id",
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)

        val sak2 = Sak(søknadId = UUIDv7.ny(), opprettet = now, sakId = behandlingskjedeId)
        sak2.knyttTilSak(
            MeldekortbehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                meldekortId = "id",
                behandlingskjedeId = behandlingskjedeId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak2)
    }

    @Test
    fun `knyttTilSak med ManuellBehandlingOpprettetHendelse`() {
        val behandlingskjedeId = UUIDv7.ny()
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now)

        sak.knyttTilSak(
            ManuellBehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                manuellId = UUIDv7.ny(),
            ),
        ) shouldBe KnyttTilSakResultat.IkkeKnyttetTilSak(sak.sakId)

        sak.leggTilBehandling(behandling)
        sak.knyttTilSak(
            ManuellBehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = behandling.behandlingId,
                manuellId = UUIDv7.ny(),
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)

        val sak2 = Sak(søknadId = UUIDv7.ny(), opprettet = now, sakId = behandlingskjedeId)
        sak2.knyttTilSak(
            ManuellBehandlingOpprettetHendelse(
                manuellId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                behandlingskjedeId = behandlingskjedeId,
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
                type = UtløstAvType.SØKNAD,
            ),
        ) shouldBe KnyttTilSakResultat.IkkeKnyttetTilSak(sak.sakId)

        sak.knyttTilSak(
            BehandlingOpprettetHendelse(
                sakId = sakId,
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                type = UtløstAvType.SØKNAD,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)
    }
}
