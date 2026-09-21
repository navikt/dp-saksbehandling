package no.nav.dagpenger.saksbehandling.tilbakekreving

import kotliquery.queryOf
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.hendelser.Tilbakekreving
import java.util.UUID

class PostgresTilbakekrevingRepository(
    private val databaseSession: DatabaseSession,
) : TilbakekrevingRepository {
    override fun lagre(
        tilbakekreving: Tilbakekreving,
        ctx: Transaksjonskontekst,
    ) {
        databaseSession.inContext(ctx) {
            session.run(
                action =
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            INSERT INTO tilbakekreving_v1 (
                                id,
                                opprettet,
                                avvent_behandling_til_dato ,
                                varsel_sendt ,
                                behandlingsstatus ,
                                forrige_behandlingsstatus ,
                                totalt_feilutbetalt_belop ,
                                saksbehandling_url ,
                                fullstendig_periode_fom ,
                                fullstendig_periode_tom
                            )
                            VALUES (
                                :id,
                                :opprettet,
                                :avvent_behandling_til_dato ,
                                :varsel_sendt ,
                                :behandlingsstatus ,
                                :forrige_behandlingsstatus ,
                                :totalt_feilutbetalt_belop ,
                                :saksbehandling_url ,
                                :fullstendig_periode_fom ,
                                :fullstendig_periode_tom
                            )
                            ON CONFLICT (id) DO UPDATE SET
                                opprettet = EXCLUDED.opprettet,
                                avvent_behandling_til_dato = EXCLUDED.avvent_behandling_til_dato,
                                varsel_sendt = EXCLUDED.varsel_sendt,
                                behandlingsstatus = EXCLUDED.behandlingsstatus,
                                forrige_behandlingsstatus = EXCLUDED.forrige_behandlingsstatus,
                                totalt_feilutbetalt_belop = EXCLUDED.totalt_feilutbetalt_belop,
                                saksbehandling_url = EXCLUDED.saksbehandling_url,
                                fullstendig_periode_fom = EXCLUDED.fullstendig_periode_fom,
                                fullstendig_periode_tom = EXCLUDED.fullstendig_periode_tom
                            """.trimIndent(),
                        paramMap =
                            mapOf(
                                "id" to tilbakekreving.behandlingId,
                                "opprettet" to tilbakekreving.opprettet,
                                "avvent_behandling_til_dato" to tilbakekreving.avventBehandlingTilDato,
                                "varsel_sendt" to tilbakekreving.varselSendt,
                                "behandlingsstatus" to tilbakekreving.behandlingsstatus.toString(),
                                "forrige_behandlingsstatus" to tilbakekreving.forrigeBehandlingsstatus?.toString(),
                                "totalt_feilutbetalt_belop" to tilbakekreving.totaltFeilutbetaltBeløp,
                                "saksbehandling_url" to tilbakekreving.saksbehandlingURL,
                                "fullstendig_periode_fom" to tilbakekreving.fullstendigPeriode.fom,
                                "fullstendig_periode_tom" to tilbakekreving.fullstendigPeriode.tom,
                            ),
                    ).asUpdate,
            )
        }
    }

    override fun hent(behandlingId: UUID): Tilbakekreving =
        databaseSession.session { session ->
            session.run(
                action =
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            SELECT 
                                id,
                                opprettet,
                                avvent_behandling_til_dato ,
                                varsel_sendt ,
                                behandlingsstatus ,
                                forrige_behandlingsstatus ,
                                totalt_feilutbetalt_belop ,
                                saksbehandling_url ,
                                fullstendig_periode_fom ,
                                fullstendig_periode_tom
                            FROM tilbakekreving_v1
                            WHERE id = :id
                            """.trimIndent(),
                        paramMap = mapOf("id" to behandlingId),
                    ).map { row ->
                        Tilbakekreving(
                            behandlingId = row.uuid("id"),
                            opprettet = row.localDateTime("opprettet"),
                            avventBehandlingTilDato = row.localDateOrNull("avvent_behandling_til_dato"),
                            varselSendt = row.localDateOrNull("varsel_sendt"),
                            behandlingsstatus =
                                Tilbakekreving.BehandlingStatus.valueOf(
                                    row.string("behandlingsstatus"),
                                ),
                            forrigeBehandlingsstatus =
                                row.stringOrNull("forrige_behandlingsstatus")?.let {
                                    Tilbakekreving.BehandlingStatus.valueOf(it)
                                },
                            totaltFeilutbetaltBeløp = row.bigDecimal("totalt_feilutbetalt_belop"),
                            saksbehandlingURL = row.string("saksbehandling_url"),
                            fullstendigPeriode =
                                Tilbakekreving.Periode(
                                    fom = row.localDate("fullstendig_periode_fom"),
                                    tom = row.localDate("fullstendig_periode_tom"),
                                ),
                        )
                    }.asSingle,
            ) ?: throw TilbakekrevingIkkeFunnet("Fant ikke tilbakekreving med id $behandlingId")
        }
}

class TilbakekrevingIkkeFunnet(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
