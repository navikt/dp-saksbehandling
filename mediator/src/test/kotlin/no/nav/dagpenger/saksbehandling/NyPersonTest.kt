package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class NyPersonTest {
    private val nå = LocalDateTime.now()
    private val oppgaveId = UUIDv7.ny()
    private val behandling1 =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
            oppgaveId = oppgaveId,
        )
    private val behandling2 =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
        )
    private val behandling3 =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
        )
    private val behandling4 =
        NyBehandling(
            behandlingId = UUIDv7.ny(),
            behandlingType = BehandlingType.RETT_TIL_DAGPENGER,
            opprettet = nå,
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

    @Test
    fun `Rekkefølge og antall ganger en unik sak legges til er likegyldig`() {
        val id = UUIDv7.ny()
        val person1 =
            NyPerson(
                id = id,
                ident = "12345678901",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            ).also {
                it.leggTilSak(sak1)
                it.leggTilSak(sak2)
            }

        val person2 =
            NyPerson(
                id = id,
                ident = "12345678901",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            ).also {
                it.leggTilSak(sak2)
                it.leggTilSak(sak1)
                it.leggTilSak(sak2)
            }

        person1 shouldBe person2
    }
}
