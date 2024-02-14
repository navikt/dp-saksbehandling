package no.nav.dagpenger.saksbehandling.db

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.saksbehandling.InntektPeriode
import no.nav.dagpenger.saksbehandling.MinsteInntektVurdering
import java.util.UUID
import javax.sql.DataSource

class VurderingPostgresRepository(private val dataSource: DataSource) : VurderingRepository {
    override fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteInntektVurdering {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT * FROM minsteinntekt_vurdering_v1 WHERE oppgaveId = :oppgaveId""",
                    paramMap = mapOf("oppgaveId" to oppgaveId),
                ).map { row ->
                    val uuid = row.uuid("uuid")
                    MinsteInntektVurdering(
                        uuid = uuid,
                        virkningsdato = row.localDate("virkningsdato"),
                        vilkaarOppfylt = row.boolean("vilkaarOppfylt"),
                        inntektsId = row.string("inntektsId"),
                        inntektPerioder = session.hentInntektPerioder(uuid),
                        subsumsjonsId = row.string("subsumsjonsId"),
                        regelIdentifikator = row.string("regelIdentifikator"),
                        beregningsregel = row.string("beregningsRegel"),
                    )
                }.asSingle,
            ) ?: throw RuntimeException("Fant ikke minsteinntekt-vurdering for oppgaveId: $oppgaveId")
        }
    }

    override fun lagreMinsteInntektVurdering(
        oppgaveId: UUID,
        vurdering: MinsteInntektVurdering,
    ) {
        TODO("Not yet implemented")
    }

    private fun Session.hentInntektPerioder(uuid: UUID): List<InntektPeriode> = TODO()
}
