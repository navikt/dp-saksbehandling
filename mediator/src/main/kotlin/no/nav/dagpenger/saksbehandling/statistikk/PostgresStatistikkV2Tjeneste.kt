package no.nav.dagpenger.saksbehandling.statistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2GruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2SerieDTO
import javax.sql.DataSource
import no.nav.dagpenger.saksbehandling.Emneknagg

class PostgresStatistikkV2Tjeneste(
    private val dataSource: DataSource,
) : StatistikkV2Tjeneste {
    override fun hentStatuserForUtløstAvFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO> {
        val utløstAvTyper =
            statistikkFilter.utløstAvTyper.ifEmpty {
                UtløstAvType.entries.toSet()
            }
        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT tils.tilstand                                                        AS status
                             , (SELECT  count(*)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand = tils.tilstand
                                AND     beha.utlost_av = ANY(:utlost_av_typer)
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <= :tom_pluss_1_dag
                             )                                                                      AS antall
                             , (SELECT  MIN(oppg.opprettet)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand = tils.tilstand
                                AND     beha.utlost_av = ANY(:utlost_av_typer)
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <= :tom_pluss_1_dag
                             )                                                                      AS eldste_oppgave
                        FROM (VALUES ('KLAR_TIL_BEHANDLING')
                                   , ('UNDER_BEHANDLING')
                                   , ('PAA_VENT')
                                   , ('KLAR_TIL_KONTROLL')
                                   , ('UNDER_KONTROLL')
                                   , ('FERDIG_BEHANDLET')
                                   , ('AVBRUTT')
                             ) AS tils(tilstand)
                        """,
                    paramMap =
                        mapOf(
                            "fom" to statistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                            "utlost_av_typer" to utløstAvTyper.map { it.name }.toTypedArray(),
                        ),
                ).map { row ->
                    StatistikkV2GruppeDTO(
                        navn = row.string("status"),
                        total = row.int("antall"),
                        eldsteOppgave = row.localDateTime("eldste_oppgave"),
                    )
                }.asList,
            )
        }
    }

    override fun hentUtløstAvSerier(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO> {
        val utløstAvTyper =
            statistikkFilter.utløstAvTyper.ifEmpty {
                UtløstAvType.entries.toSet()
            }
        val tilstander =
            statistikkFilter.statuser.ifEmpty {
                Oppgave.Tilstand.Type.entries
                    .map { it.name }
                    .toSet()
            }
        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT beh.utlost_av, oppg.tilstand, COUNT(*) AS count
                        FROM oppgave_v1 oppg
                                 JOIN behandling_v1 beh ON beh.id = oppg.behandling_id
                        WHERE beh.utlost_av = ANY(:utlost_av_typer)
                          AND oppg.tilstand = ANY(:tilstander)
                          AND oppg.opprettet >= :fom
                          AND oppg.opprettet <= :tom_pluss_1_dag
                        GROUP BY beh.utlost_av, oppg.tilstand;
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "utlost_av_typer" to utløstAvTyper.map { it.name }.toTypedArray(),
                            "tilstander" to tilstander.toTypedArray(),
                            "fom" to statistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                        ),
                ).map { row ->
                    StatistikkV2SerieDTO(
                        navn = row.string("utlost_av"),
                        verdier = listOf(row.int("count")),
                    )
                }.asList,
            )
        }
    }

    override fun hentStatuserForRettighetstypeFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO> {
//        val utløstAvTyper =
//            statistikkFilter.utløstAvTyper.ifEmpty {
//                UtløstAvType.entries.toSet()
//            }
//        val tilstander =
//            statistikkFilter.statuser.ifEmpty {
//                Oppgave.Tilstand.Type.entries
//                    .map { it.name }
//                    .toSet()
//            }
        val rettighetstyper =
            statistikkFilter.rettighetstyper.ifEmpty {
                setOf(
                    Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn,
                    Emneknagg.Regelknagg.RETTIGHET_PERMITTERT.visningsnavn,
                    Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK.visningsnavn,
                    Emneknagg.Regelknagg.RETTIGHET_KONKURS.visningsnavn
                )

            }

        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT tils.tilstand                                                        AS status
                             , (SELECT  count(*)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand = tils.tilstand
                                AND     beha.utlost_av = 'SØKNAD'
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <= :tom_pluss_1_dag
                                AND EXISTS(
                                    SELECT  1
                                    FROM    emneknagg_v1 emne
                                    WHERE   emne.oppgave_id = oppg.id
                                    AND     emne.emneknagg  = ANY(:rettighetstyper)
                                )
                             )                                                                      AS antall
                             , (SELECT  MIN(oppg.opprettet)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand = tils.tilstand
                                AND     beha.utlost_av = 'SØKNAD'
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <= :tom_pluss_1_dag
                                AND EXISTS(
                                    SELECT  1
                                    FROM    emneknagg_v1 emne
                                    WHERE   emne.oppgave_id = oppg.id
                                    AND     emne.emneknagg  = ANY(:rettighetstyper)
                                )
                             )                                                                      AS eldste_oppgave
                        FROM (VALUES ('KLAR_TIL_BEHANDLING')
                                   , ('UNDER_BEHANDLING')
                                   , ('PAA_VENT')
                                   , ('KLAR_TIL_KONTROLL')
                                   , ('UNDER_KONTROLL')
                                   , ('FERDIG_BEHANDLET')
                                   , ('AVBRUTT')
                             ) AS tils(tilstand)
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "fom" to statistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                            "rettighetstyper" to rettighetstyper.toTypedArray(),
                        ),
                ).map { row ->
                    StatistikkV2GruppeDTO(
                        navn = row.string("tilstand"),
                        total = row.int("count"),
                        eldsteOppgave = row.localDateTime("eldste_oppgave"),
                    )
                }.asList,
            )
        }
    }

    override fun hentRettighetstypeSerier(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO> {
        val utløstAvTyper =
            statistikkFilter.utløstAvTyper.ifEmpty {
                UtløstAvType.entries.toSet()
            }

        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT emn.emneknagg, COUNT(*) AS count
                        FROM oppgave_v1 oppg
                            JOIN behandling_v1 beh ON beh.id = oppg.behandling_id
                            JOIN emneknagg_v1 emn ON oppg.id = emn.oppgave_id
                        WHERE beh.utlost_av = ANY(:utlost_av_typer)
                          AND oppg.opprettet >= :fom
                          AND oppg.opprettet <= :tom_pluss_1_dag
                        GROUP BY emn.emneknagg;
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "utlost_av_typer" to utløstAvTyper.map { it.name }.toTypedArray(),
                            "fom" to statistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                        ),
                ).map { row ->
                    StatistikkV2SerieDTO(
                        navn = row.string("emneknagg"),
                        verdier = listOf(row.int("count")),
                    )
                }.asList,
            )
        }
    }
}
