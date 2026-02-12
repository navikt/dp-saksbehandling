package no.nav.dagpenger.saksbehandling.statistikk.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.statistikk.ProduksjonsstatistikkFilter
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresProduksjonsstatistikkRepository(
    private val dataSource: DataSource,
) : ProduksjonsstatistikkRepository {
    override fun hentAntallBrevSendt(): Int =
        sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        select  count(*)  as count from utsending_v1 where  tilstand =  'Distribuert' ;
                    """,
                    paramMap = mapOf(),
                ).map { row ->
                    row.int("count")
                }.asSingle,
            )
        } ?: 0

    override fun hentTilstanderMedUtløstAvFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<TilstandStatistikk> {
        val utløstAvTyper =
            produksjonsstatistikkFilter.utløstAvTyper.ifEmpty {
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
                            "fom" to produksjonsstatistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to produksjonsstatistikkFilter.periode.tom.plusDays(1),
                            "utlost_av_typer" to utløstAvTyper.map { it.name }.toTypedArray(),
                        ),
                ).map { row ->
                    TilstandStatistikk(
                        tilstand = Oppgave.Tilstand.Type.valueOf(row.string("tilstand")),
                        antall = row.int("antall"),
                        eldsteOppgaveTidspunkt = row.localDateTimeOrNull("eldste_oppgave"),
                    )
                }.asList,
            )
        }
    }

    override fun hentUtløstAvMedTilstandFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<AntallOppgaverForUtløstAv> {
        val tilstander =
            produksjonsstatistikkFilter.tilstander.ifEmpty {
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
                                   , ('KLAGE')
                                   , ('INNSENDING')
                             ) AS utlo(utlost_av)
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "tilstander" to tilstander.map { it.name }.toTypedArray(),
                            "fom" to produksjonsstatistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to produksjonsstatistikkFilter.periode.tom.plusDays(1),
                        ),
                ).map { row ->
                    AntallOppgaverForUtløstAv(
                        utløstAv = UtløstAvType.valueOf(row.string("utlost_av")),
                        antall = row.int("antall"),
                    )
                }.asList,
            )
        }
    }

    override fun hentTilstanderMedRettighetFilter(produksjonsstatistikkFilter: ProduksjonsstatistikkFilter): List<TilstandStatistikk> {
        val rettighetstyper =
            produksjonsstatistikkFilter.rettighetstyper.ifEmpty {
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
                            "fom" to produksjonsstatistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to produksjonsstatistikkFilter.periode.tom.plusDays(1),
                            "rettighetstyper" to rettighetstyper.toTypedArray(),
                        ),
                ).map { row ->
                    TilstandStatistikk(
                        tilstand = Oppgave.Tilstand.Type.valueOf(row.string("tilstand")),
                        antall = row.int("antall"),
                        eldsteOppgaveTidspunkt = row.localDateTimeOrNull("eldste_oppgave"),
                    )
                }.asList,
            )
        }
    }

    override fun hentRettigheterMedTilstandFilter(
        produksjonsstatistikkFilter: ProduksjonsstatistikkFilter,
    ): List<AntallOppgaverForRettighet> {
        val tilstander =
            produksjonsstatistikkFilter.tilstander.ifEmpty {
                Oppgave.Tilstand.Type.entries
                    .toSet()
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
                            "tilstander" to tilstander.map { it.name }.toTypedArray(),
                            "fom" to produksjonsstatistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to produksjonsstatistikkFilter.periode.tom.plusDays(1),
                        ),
                ).map { row ->
                    AntallOppgaverForRettighet(
                        rettighet = row.string("rettighet"),
                        antall = row.int("antall"),
                    )
                }.asList,
            )
        }
    }

    override fun hentResultatSerierForUtløstAv(
        produksjonsstatistikkFilter: ProduksjonsstatistikkFilter,
    ): List<AntallOppgaverForTilstandOgUtløstAv> {
        val utløstAvTyper =
            produksjonsstatistikkFilter.utløstAvTyper.ifEmpty {
                UtløstAvType.entries.toSet()
            }
        val tilstander =
            produksjonsstatistikkFilter.tilstander.ifEmpty {
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
                            )   AS antall
                        FROM        unnest(:tilstander) AS tils(tilstand)
                        CROSS JOIN  unnest(:utlost_av_typer) AS utlo(utlost_av)
                        """,
                    paramMap =
                        mapOf(
                            "fom" to produksjonsstatistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to produksjonsstatistikkFilter.periode.tom.plusDays(1),
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

    override fun hentResultatSerierForRettigheter(
        produksjonsstatistikkFilter: ProduksjonsstatistikkFilter,
    ): List<AntallOppgaverForTilstandOgRettighet> {
        val rettighetstyper =
            produksjonsstatistikkFilter.rettighetstyper.ifEmpty {
                setOf(
                    "Ordinær",
                    "Verneplikt",
                    "Permittert",
                    "Permittert fisk",
                    "Konkurs",
                )
            }
        val tilstander =
            produksjonsstatistikkFilter.tilstander.ifEmpty {
                Oppgave.Tilstand.Type.entries
                    .toSet()
            }
        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT  tils.tilstand,
                                rett.rettighet,
                            (   SELECT  COUNT(*)
                                FROM    oppgave_v1 oppg
                                JOIN    behandling_v1 beha ON beha.id = oppg.behandling_id
                                WHERE   oppg.tilstand   = tils.tilstand
                                AND     beha.utlost_av  = 'SØKNAD'
                                AND     oppg.opprettet >= :fom
                                AND     oppg.opprettet <  :tom_pluss_1_dag
                                AND EXISTS
                                (   SELECT  1
                                    FROM    emneknagg_v1 emne
                                    WHERE   emne.oppgave_id = oppg.id
                                    AND     emne.emneknagg  = rett.rettighet
                                )
                             ) AS antall
                        FROM        unnest(:tilstander) AS tils(tilstand)
                        CROSS JOIN  unnest(:rettighet_typer) AS rett(rettighet)
                        """,
                    paramMap =
                        mapOf(
                            "fom" to produksjonsstatistikkFilter.periode.fom,
                            "tom_pluss_1_dag" to produksjonsstatistikkFilter.periode.tom.plusDays(1),
                            "rettighet_typer" to rettighetstyper.toTypedArray(),
                            "tilstander" to tilstander.map { it.name }.toTypedArray(),
                        ),
                ).map { row ->
                    AntallOppgaverForTilstandOgRettighet(
                        tilstand = Oppgave.Tilstand.Type.valueOf(row.string("tilstand")),
                        rettighet = row.string("rettighet"),
                        antall = row.int("antall"),
                    )
                }.asList,
            )
        }
    }
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

data class AntallOppgaverForRettighet(
    val rettighet: String,
    val antall: Int,
)

data class AntallOppgaverForUtløstAv(
    val utløstAv: UtløstAvType,
    val antall: Int,
)

data class TilstandStatistikk(
    val tilstand: Oppgave.Tilstand.Type,
    val antall: Int,
    val eldsteOppgaveTidspunkt: LocalDateTime?,
)
