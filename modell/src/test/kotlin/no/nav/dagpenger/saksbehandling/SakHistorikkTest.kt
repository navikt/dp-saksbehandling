package no.nav.dagpenger.saksbehandling
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SakHistorikkTest {
    private val nå = LocalDateTime.now()
    private val oppgaveId = UUIDv7.ny()
    private val behandling1 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = HendelseBehandler.DpBehandling.Søknad,
            opprettet = nå,
            oppgaveId = oppgaveId,
            hendelse = TomHendelse,
        )
    private val behandling2 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = HendelseBehandler.DpBehandling.Søknad,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val behandling3 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = HendelseBehandler.DpBehandling.Søknad,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val behandling4 =
        Behandling(
            behandlingId = UUIDv7.ny(),
            utløstAv = HendelseBehandler.DpBehandling.Søknad,
            opprettet = nå,
            hendelse = TomHendelse,
        )
    private val sak1 =
        Sak(
            opprettet = nå,
        ).also {
            it.leggTilBehandling(behandling1)
            it.leggTilBehandling(behandling2)
        }
    private val sak2 =
        Sak(
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
            inhabileNavIdenter = emptyList(),
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

        val sakHistorikk2 =
            SakHistorikk(
                person = person,
            ).also {
                it.leggTilSak(sak2)
                it.leggTilSak(sak1)
                it.leggTilSak(sak2)
            }

        sakHistorikk1 shouldBe sakHistorikk2
    }

    @Test
    fun `SakHistorikk med ulike type saker`() {
        val sakHistorikk = ModellTestHelper.lagSakHistorikk()
        sakHistorikk.ferietilleggSaker() shouldBe listOf(ModellTestHelper.ferietilleggSak)
        sakHistorikk.dagpengeSaker() shouldBe listOf(ModellTestHelper.dagpengeSak)
    }

    @Test
    fun `Skal kunne fjerne en behandling fra en sak`() {
        val sakHistorikk = ModellTestHelper.lagSakHistorikk()
        val sak = sakHistorikk.dagpengeSaker().first()
        val behandlingId = sak.behandlinger().first().behandlingId
        sak.fjernBehandling(behandlingId)
        sak.behandlinger().any { it.behandlingId == behandlingId } shouldBe false
    }
}
