package no.nav.dagpenger.saksbehandling.frist

import no.nav.dagpenger.saksbehandling.Oppgave.PaaVent
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppgaveFristUtgåttJobTest {
    @Test
    fun `Hugga bugga`() = withMigratedDb { db ->

        val oppgave = lagOppgave(
            tilstand = PaaVent,
            utsattTil = LocalDate.now().minusDays(1)
        )
        val oppgave2 = lagOppgave(
            tilstand = PaaVent,
            utsattTil = LocalDate.now()
        )



        settOppgaverMedUtgåttFristTilKlarTilBehandling()

    }




    }
}
