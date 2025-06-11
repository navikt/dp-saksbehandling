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
    private val nyPerson =
        NyPerson(
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        ).also {
            it.leggTilSak(sak1)
            it.leggTilSak(sak2)
        }

    @Test
    fun hubba() {
        val person =
            NyPerson(
                id = nyPerson.id,
                ident = nyPerson.ident,
                skjermesSomEgneAnsatte = nyPerson.skjermesSomEgneAnsatte,
                adressebeskyttelseGradering = nyPerson.adressebeskyttelseGradering,
            ).also {
                it.leggTilSak(
                    NySak(
                        sakId = sak1.sakId,
                        søknadId = sak1.søknadId,
                        opprettet = sak1.opprettet,
                        behandlinger = mutableListOf(behandling1, behandling2),
                    ),
                )
                it.leggTilSak(
                    NySak(
                        sakId = sak2.sakId,
                        søknadId = sak2.søknadId,
                        opprettet = sak2.opprettet,
                        behandlinger = mutableListOf(behandling3, behandling4),
                    ),
                )
            }

        person shouldBe nyPerson
    }
}
