package no.nav.dagpenger.saksbehandling.statistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2GruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2SerieDTO
import javax.sql.DataSource

interface StatistikkV2Tjeneste {
    fun hentRettighetstyper(navIdent: String, statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO>;
    fun hentRettighetstypeSerier(navIdent: String, statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO>;
    fun hentOppgavetyper(navIdent: String, statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO>;
    fun hentOppgavetypeSerier(navIdent: String, statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO>;
}

class PostgresStatistikkV2Tjeneste(
    private val dataSource: DataSource,
) : StatistikkV2Tjeneste {

    override fun hentRettighetstyper(
        navIdent: String,
        statistikkFilter: StatistikkFilter
    ): List<StatistikkV2GruppeDTO> =
        sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    SELECT beha.utlost_av, COUNT(*) AS count, MIN(oppg.opprettet) AS eldste_oppgave
                    FROM oppgave_v1 oppg
                    JOIN behandling_v1 beha ON beha.id = oppg.behandling_id
                    WHERE beha.utlost_av IN (:utlostAvTyper)
                    AND saksbehandler_ident = :navIdent
                    AND oppg.opprettet >= :fom
                    AND oppg.opprettet <= :tom_pluss_1_dag
                    GROUP BY beha.utlost_av;
                    """,
                    paramMap =
                        mapOf(
                            "navIdent" to navIdent,
                            "fom" to statistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                            "utlostAvTyper" to statistikkFilter.utløstAvTyper.toList(),
                        )
                ),
            ).map { row ->
                StatistikkV2GruppeDTO(
                    navn = row.string("rettighet_type"),
                    total = row.int("count"),
                    eldsteOppgave = row.localDateTime("eldste_oppgave"),
                )
            }.asList
        }

    override fun hentRettighetstypeSerier(
        navIdent: String,
        statistikkFilter: StatistikkFilter
    ): List<StatistikkV2SerieDTO> {
        TODO("Not yet implemented")
    }

    override fun hentOppgavetyper(
        navIdent: String,
        statistikkFilter: StatistikkFilter
    ): List<StatistikkV2GruppeDTO> =
        sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT oppg.tilstand, COUNT(*) AS count, MIN(oppg.opprettet) AS eldste_oppgave
                        FROM oppgave_v1 oppg
                        WHERE oppg.tilstand IN (:tilstander)
                          AND oppg.opprettet >= :fom
                          AND oppg.opprettet <= :tom_pluss_1_dag
                        GROUP BY oppg.tilstand;
                    """.trimIndent(),
                    paramMap = mapOf(
                        "tilstander" to statistikkFilter.statuser,
                        "fom" to statistikkFilter.periode.fom,
                        "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                    )
                )
            )
        }.map { row ->
            StatistikkV2GruppeDTO(
                navn = row.string("tilstand"),
                total = row.int("count"),
                eldsteOppgave = row.localDateTime("eldste_oppgave"),
            )
        }.asList

    override fun hentOppgavetypeSerier(
        navIdent: String,
        statistikkFilter: StatistikkFilter
    ): List<StatistikkV2SerieDTO> {
        TODO("Not yet implemented")
    }

}
