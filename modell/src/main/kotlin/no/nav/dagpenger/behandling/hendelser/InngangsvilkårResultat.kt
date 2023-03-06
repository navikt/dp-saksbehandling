package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.entitet.Arbeidstimer
import java.time.LocalDate
import java.util.UUID

abstract sealed class InngangsvilkårResultat(
    ident: String,
    val vilkårsvurderingId: UUID,
    val virkningsdato: LocalDate,
) : VilkårResultatHendelse(vilkårsvurderingId, ident)

class Innvilget(
    ident: String,
    vilkårsvurderingId: UUID,
    virkningsdato: LocalDate,
    val fastsattArbeidstidPerDag: Arbeidstimer,
) : InngangsvilkårResultat(ident, vilkårsvurderingId, virkningsdato)

class Avslått(
    ident: String,
    vilkårsvurderingId: UUID,
    virkningsdato: LocalDate,
) : InngangsvilkårResultat(ident, vilkårsvurderingId, virkningsdato)
