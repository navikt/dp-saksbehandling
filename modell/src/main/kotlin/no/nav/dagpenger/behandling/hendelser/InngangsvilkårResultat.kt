package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.entitet.Arbeidstimer.Companion.arbeidstimer
import java.time.LocalDate
import java.util.UUID

sealed class InngangsvilkårResultat(
    ident: String,
    val vilkårsvurderingId: UUID,
    val virkningsdato: LocalDate,
) : VilkårResultatHendelse(vilkårsvurderingId, ident)

class Innvilget(
    ident: String,
    vilkårsvurderingId: UUID,
    virkningsdato: LocalDate,
    val fastsattArbeidstidPerDag: Number,
) : InngangsvilkårResultat(ident, vilkårsvurderingId, virkningsdato) {

    internal fun fastsattArbeidstidPerDag() = fastsattArbeidstidPerDag.arbeidstimer
}

class Avslått(
    ident: String,
    vilkårsvurderingId: UUID,
    virkningsdato: LocalDate,
) : InngangsvilkårResultat(ident, vilkårsvurderingId, virkningsdato)
