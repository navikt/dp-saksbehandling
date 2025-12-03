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
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now)

        sak.knyttTilSak(
            SøknadsbehandlingOpprettetHendelse(
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = null,
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
    }

    @Test
    fun `knyttTilSak med MeldekortbehandlingOpprettetHendelse`() {
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now)

        sak.knyttTilSak(
            MeldekortbehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                meldekortId = "id",
                behandlingskjedeId = UUIDv7.ny(),
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
                behandlingskjedeId = sak.sakId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)
    }

    @Test
    fun `knyttTilSak med ManuellBehandlingOpprettetHendelse`() {
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now)

        sak.knyttTilSak(
            ManuellBehandlingOpprettetHendelse(
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                basertPåBehandling = UUIDv7.ny(),
                manuellId = UUIDv7.ny(),
                behandlingskjedeId = UUIDv7.ny(),
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
                behandlingskjedeId = sak.sakId,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)
    }

    @Test
    fun `knyttTilSak med BehandlingOpprettetHendelse`() {
        val sak = Sak(søknadId = UUIDv7.ny(), opprettet = now)

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
                sakId = sak.sakId,
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = now,
                type = UtløstAvType.SØKNAD,
            ),
        ) shouldBe KnyttTilSakResultat.KnyttetTilSak(sak)
    }
}
