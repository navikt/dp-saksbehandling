package no.nav.dagpenger.saksbehandling.statistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

interface StatistikkV2Tjeneste {
    fun hentSaksbehandlerStatistikk(
        navIdent: String,
        statistikkFilter: StatistikkFilter,
    ): Int

    fun hentAntallVedtakGjort(statistikkFilter: StatistikkFilter): Int

    fun hentAntallBrevSendt(statistikkFilter: StatistikkFilter): Int
}

class PostgresStatistikkV2Tjeneste(
    private val dataSource: DataSource,
) : StatistikkV2Tjeneste {
    override fun hentSaksbehandlerStatistikk(
        navIdent: String,
        statistikkFilter: StatistikkFilter,
    ): Int =
        sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    WITH filtrert_data AS (SELECT *
                                           FROM oppgave_v1
                                           WHERE tilstand = 'FERDIG_BEHANDLET'
                                           AND saksbehandler_ident = :navIdent)
                    SELECT
                    -- Count for the current day
                        (SELECT COUNT(*)
                         FROM filtrert_data
                         WHERE endret_tidspunkt >= :fom
                         AND endret_tidspunkt <= :tom) AS count;
                    """,
                    paramMap =
                        mapOf(
                            "navIdent" to navIdent,
                            "fom" to statistikkFilter.periode.fom,
                            "tom" to statistikkFilter.periode.tom,
                        ),
                ).map { row ->
                    row.int("count")
                }.asSingle,
            )
        } ?: 0

    override fun hentAntallVedtakGjort(statistikkFilter: StatistikkFilter): Int =
        sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT COUNT(*) AS count
                        FROM oppgave_v1
                        WHERE tilstand = 'FERDIG_BEHANDLET'
                        AND endret_tidspunkt >= :fom
                        AND endret_tidspunkt <= :tom;
                    """,
                    paramMap = mapOf("fom" to statistikkFilter.periode.fom, "tom" to statistikkFilter.periode.tom),
                ).map { row ->
                    row.int("count")
                }.asSingle,
            )
        } ?: 0

    override fun hentAntallBrevSendt(statistikkFilter: StatistikkFilter): Int =
        sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT count(*) AS count
                        FROM utsending_v1
                        WHERE tilstand = 'Distribuert'
                        AND registrert_tidspunkt >= :fom
                        AND registrert_tidspunkt <= :tom;
                    """,
                    paramMap = mapOf(),
                ).map { row ->
                    row.int("count")
                }.asSingle,
            )
        } ?: 0
}
