package no.nav.dagpenger.saksbehandling.klage

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilJson
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import javax.sql.DataSource
import kotlin.random.Random

class OversendKlageinstansAlarmRepositoryTest {
    @Test
    fun `Skal hente oversendelser til klageinstans som ikke er ferdigstilt innen 1 time`() {
        val nå = LocalDateTime.now()
        val enTimeSiden = nå.minusHours(1)
        val littMerEnnEnTimeSiden = nå.minusHours(1)
        withMigratedDb { ds ->
            val repository = OversendKlageinstansAlarmRepository(ds)
            val klageBehandlingLittMerEnnEnTimeSiden = ds.opprettOgLagreKlageBehandling(KlageBehandling.OversendKlageinstans, littMerEnnEnTimeSiden)

            val ventendeOversendelser = repository.hentVentendeOversendelser(intervallAntallTimer = 1)
            ventendeOversendelser.size shouldBe 1
            ventendeOversendelser.map { it.tilstand } shouldBe
                    listOf(
                        KlageBehandling.OversendKlageinstans,
                    )
        }
    }

    private fun DataSource.opprettOgLagreKlageBehandling(
        tilstand: KlageBehandling.KlageTilstand,
        tidspunkt: LocalDateTime = LocalDateTime.now(),
    ): KlageBehandling {
        val klageBehandling = KlageBehandling.rehydrer(
            behandlingId = UUIDv7.ny(),
            journalpostId = Random.nextInt(1,1000).toString(),
            tilstand = tilstand,
            behandlendeEnhet = "4408",
        )

        sessionOf(this).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO klage_v1
                            ( id,  tilstand,  registrert_tidspunkt,  endret_tidspunkt,  journalpost_id,  behandlende_enhet,  opplysninger)
                        VALUES 
                            (:id, :tilstand, :registrert_tidspunkt, :endret_tidspunkt, :journalpost_id, :behandlende_enhet, :opplysninger)
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to klageBehandling.behandlingId,
                            "tilstand" to tilstand.type.name,
                            "registrert_tidspunkt" to tidspunkt,
                            "endret_tidspunkt" to tidspunkt,
                            "journalpost_id" to klageBehandling.journalpostId(),
                            "behandlende_enhet" to klageBehandling.behandlendeEnhet(),
                            "opplysninger" to
                                    PGobject().also {
                                        it.type = "JSONB"
                                        it.value = klageBehandling.alleOpplysninger().tilJson()
                                    },
                        ),
                ).asUpdate,
            )
        }
        return klageBehandling
    }
}
