package no.nav.dagpenger.saksbehandling.statistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2GruppeDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkV2SerieDTO
import no.nav.dagpenger.saksbehandling.api.models.V2StatusNavnDTO
import javax.sql.DataSource

class PostgresStatistikkV2Tjeneste(
    private val dataSource: DataSource,
) : StatistikkV2Tjeneste {
    override fun hentTilstanderMedUtløstAvFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO> {
        val utløstAvTyper =
            statistikkFilter.utløstAvTyper.ifEmpty {
                UtløstAvType.entries.toSet()
            }

        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT tils.tilstand                                                        AS tilstand
                             , (SELECT  COUNT(*)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand   = tils.tilstand
                                AND     beha.utlost_av  = ANY(:utlost_av_typer)
                                AND     oppg.opprettet  >= :fom
                                AND     oppg.opprettet  <  :tom_pluss_1_dag
                             )                                                                      AS antall
                             , (SELECT  MIN(oppg.opprettet)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand   = tils.tilstand
                                AND     beha.utlost_av  = ANY(:utlost_av_typer)
                                AND     oppg.opprettet  >= :fom
                                AND     oppg.opprettet  <  :tom_pluss_1_dag
                             )                                                                      AS eldste_oppgave
                        FROM (VALUES ('KLAR_TIL_BEHANDLING')
                                   , ('PAA_VENT')
                                   , ('UNDER_BEHANDLING')
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
                        navn = row.string("tilstand"),
                        total = row.int("antall"),
                        eldsteOppgave = row.localDateTimeOrNull("eldste_oppgave"),
                    )
                }.asList,
            )
        }
    }

    override fun hentUtløstAvMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO> {
        val tilstander =
            statistikkFilter.tilstander.ifEmpty {
                Oppgave.Tilstand.Type.entries
                    .toSet()
            }
        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT utlo.utlost_av                                               AS utlost_av
                             , (SELECT  COUNT(*)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   beha.utlost_av  = utlo.utlost_av
                                AND     oppg.tilstand   = ANY(:tilstander)
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <  :tom_pluss_1_dag
                             )                                                              AS antall
                        FROM (VALUES ('SØKNAD')
                                   , ('MELDEKORT')
                                   , ('MANUELL')
                                   , ('OMGJØRING')
                                   , ('KLAGE')
                                   , ('INNSENDING')
                             ) AS utlo(utlost_av)
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "tilstander" to tilstander.map { it.name }.toTypedArray(),
                            "fom" to statistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                        ),
                ).map { row ->
                    StatistikkV2SerieDTO(
                        navn = row.string("utlost_av"),
                        total = row.int("antall"),
                    )
                }.asList,
            )
        }
    }

    override fun hentTilstanderMedRettighetFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2GruppeDTO> {
        val rettighetstyper =
            statistikkFilter.rettighetstyper.ifEmpty {
                setOf(
                    Emneknagg.Regelknagg.RETTIGHET_ORDINÆR.visningsnavn,
                    Emneknagg.Regelknagg.RETTIGHET_VERNEPLIKT.visningsnavn,
                    Emneknagg.Regelknagg.RETTIGHET_PERMITTERT.visningsnavn,
                    Emneknagg.Regelknagg.RETTIGHET_PERMITTERT_FISK.visningsnavn,
                    Emneknagg.Regelknagg.RETTIGHET_KONKURS.visningsnavn,
                )
            }

        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT tils.tilstand                                                AS tilstand
                             , (SELECT  COUNT(*)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand = tils.tilstand
                                AND     beha.utlost_av = 'SØKNAD'
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <  :tom_pluss_1_dag
                                AND EXISTS(
                                    SELECT  1
                                    FROM    emneknagg_v1 emne
                                    WHERE   emne.oppgave_id = oppg.id
                                    AND     emne.emneknagg  = ANY(:rettighetstyper)
                                )
                             )                                                              AS antall
                             , (SELECT  MIN(oppg.opprettet)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand = tils.tilstand
                                AND     beha.utlost_av = 'SØKNAD'
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <  :tom_pluss_1_dag
                                AND EXISTS(
                                    SELECT  1
                                    FROM    emneknagg_v1 emne
                                    WHERE   emne.oppgave_id = oppg.id
                                    AND     emne.emneknagg  = ANY(:rettighetstyper)
                                )
                             )                                                              AS eldste_oppgave
                        FROM (VALUES ('KLAR_TIL_BEHANDLING')
                                   , ('PAA_VENT')
                                   , ('UNDER_BEHANDLING')
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
                        total = row.int("antall"),
                        eldsteOppgave = row.localDateTimeOrNull("eldste_oppgave"),
                    )
                }.asList,
            )
        }
    }

    override fun hentRettigheterMedTilstandFilter(statistikkFilter: StatistikkFilter): List<StatistikkV2SerieDTO> {
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
                        SELECT rett.rettighet                                               AS rettighet
                             , (SELECT  COUNT(*)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON  beha.id         = oppg.behandling_id
                                JOIN    emneknagg_v1  emne ON  emne.oppgave_id = oppg.id
                                                           AND emne.emneknagg  = rett.rettighet
                                WHERE   beha.utlost_av  = 'SØKNAD'
                                AND     oppg.tilstand   = ANY(:tilstander)
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <  :tom_pluss_1_dag
                             )                                                              AS antall
                        FROM (VALUES ('Ordinær')
                                   , ('Verneplikt')
                                   , ('Permittert')
                                   , ('Permittert fisk')
                                   , ('Konkurs')
                             ) AS rett(rettighet)
                        
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "utlost_av_typer" to utløstAvTyper.map { it.name }.toTypedArray(),
                            "fom" to statistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                        ),
                ).map { row ->
                    StatistikkV2SerieDTO(
                        navn = row.string("rettighet"),
                        total = row.int("antall"),
                    )
                }.asList,
            )
        }
    }

    override fun hentResultatGrupper(statistikkFilter: StatistikkFilter): List<V2StatusNavnDTO> =
        statistikkFilter.utløstAvTyper
            .ifEmpty {
                UtløstAvType.entries.toSet()
            }.map { V2StatusNavnDTO(navn = it.name) }

    override fun hentResultatSerierForUtløstAv(statistikkFilter: StatistikkFilter): List<AntallOppgaverForTilstandOgUtløstAv> {
        val utløstAvTyper =
            statistikkFilter.utløstAvTyper.ifEmpty {
                UtløstAvType.entries.toSet()
            }
        val tilstander =
            statistikkFilter.tilstander.ifEmpty {
                Oppgave.Tilstand.Type.entries
                    .toSet()
            }
        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT  tils.tilstand,
                                utlo.utlost_av,
                                (   SELECT  COUNT(*)
                                    FROM    oppgave_v1 oppg
                                    JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                    WHERE   oppg.tilstand   = tils.tilstand
                                    AND     beha.utlost_av  = utlo.utlost_av
                                    AND     oppg.opprettet >= :fom
                                    AND     oppg.opprettet <  :tom_pluss_1_dag
                             ) AS antall
                        FROM unnest(:tilstander) AS tils(tilstand)
                        CROSS JOIN unnest(:utlost_av_typer) AS utlo(utlost_av)
                        """,
                    paramMap =
                        mapOf(
                            "fom" to statistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to statistikkFilter.periode.tom.plusDays(1),
                            "utlost_av_typer" to utløstAvTyper.map { it.name }.toTypedArray(),
                            "tilstander" to tilstander.map { it.name }.toTypedArray(),
                        ),
                ).map { row ->
                    AntallOppgaverForTilstandOgUtløstAv(
                        tilstand = Oppgave.Tilstand.Type.valueOf(row.string("tilstand")),
                        utløstAv = UtløstAvType.valueOf(row.string("utlost_av")),
                        antall = row.int("antall"),
                    )
                }.asList,
            )
        }
    }

    override fun hentResultatSerierForRettigheter(statistikkFilter: StatistikkFilter): List<AntallOppgaverForTilstandOgRettighet> =
        emptyList()
}

data class AntallOppgaverForTilstandOgUtløstAv(
    val tilstand: Oppgave.Tilstand.Type,
    val utløstAv: UtløstAvType,
    val antall: Int,
)

data class AntallOppgaverForTilstandOgRettighet(
    val tilstand: Oppgave.Tilstand.Type,
    val rettighet: String,
    val antall: Int,
)
