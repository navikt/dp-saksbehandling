package no.nav.dagpenger.behandling.db

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.behandling.InntektPeriode
import no.nav.dagpenger.behandling.MinsteinntektVurdering
import java.util.UUID
import javax.sql.DataSource

class VurderingPostgresRepository(private val ds: DataSource) : VurderingRepository {
    override fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteinntektVurdering {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT * FROM minsteinntekt_vurdering_v1 WHERE oppgaveId = :oppgaveId""",
                    paramMap = mapOf("oppgaveId" to oppgaveId),
                ).map { row ->
                    val uuid = row.uuid("uuid")
                    MinsteinntektVurdering(
                        uuid = uuid,
                        virkningsdato = row.localDate("virkningsdato"),
                        vilkaarOppfylt = row.boolean("vilkaarOppfylt"),
                        inntektsId = row.string("inntektsId"),
                        inntektPerioder = session.hentInntektPerioder(uuid),
                        subsumsjonsId = row.string("subsumsjonsId"),
                        regelIdentifikator = row.string("regelIdentifikator"),
                        beregningsRegel = row.string("beregningsRegel"),
                    )
                }.asSingle,
            ) ?: throw RuntimeException("Fanit ikke vurdering for oppgaveId: $oppgaveId")
        }
    }

    override fun lagreMinsteinntektVurdering(
        oppgaveId: UUID,
        vurdering: MinsteinntektVurdering,
    ) {
        TODO("Not yet implemented")
    }

    private fun Session.hentInntektPerioder(uuid: UUID): List<InntektPeriode> = TODO()
}
