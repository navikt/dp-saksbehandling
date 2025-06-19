package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SaksHistorikkTest {
    private val nå = LocalDateTime.now()
    private val oppgaveId = UUIDv7.ny()
    private val behandling1 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            type = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
            oppgaveId = oppgaveId,
            hendelse = TomHendelse,
        )
    private val behandling2 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            type = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val behandling3 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            type = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val behandling4 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            type = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val sak1 =
        NySak(
            søknadId = UUIDv7.ny(),
            opprettet = nå,
        ).also {
            it.leggTilBehandling(behandling1)
            it.leggTilBehandling(behandling2)
        }
    private val sak2 =
        NySak(
            søknadId = UUIDv7.ny(),
            opprettet = nå,
        ).also {
            it.leggTilBehandling(behandling3)
            it.leggTilBehandling(behandling4)
        }

    private val person =
        Person(
            id = UUIDv7.ny(),
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        )

    @Test
    fun `Rekkefølge og antall ganger en unik sak legges til er likegyldig`() {
        val sakHistorikk1 =
            SakHistorikk(
                person = person,
            ).also {
                it.leggTilSak(sak1)
                it.leggTilSak(sak2)
            }

        val saksHistorikk2 =
            SakHistorikk(
                person = person,
            ).also {
                it.leggTilSak(sak2)
                it.leggTilSak(sak1)
                it.leggTilSak(sak2)
            }

        sakHistorikk1 shouldBe saksHistorikk2
    }
}
