package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Test

class RegelmotorTest {
    @Test
    fun foobar() {
        /*
        val rett = Opplysningstype<Boolean>("Rett til dagpenger")
        val vilkår = Opplysningstype("Vilkår", rett)

        val vilkårMinsteinntekt = Opplysningstype("Krav til minsteinntekt", vilkår)
        val vilkårMaksalder = Opplysningstype("Maksalder", vilkår)

        val opplysninger = Opplysninger()
        opplysninger.leggTil(Faktum(vilkårMaksalder, true))

        val nedreTerskelFaktor = Opplysningstype<Double>("Nedre terskelfaktor") { Faktum(this, 1.5) }
        val øvreTerskelFaktor = Opplysningstype<Double>("Øvre terskelfaktor") { 3.0 }
        val grunnbeløp = Opplysningstype<Int>("Grunnbeløp") { 99858 }
        val nedreTerskelInntekt =
            Opplysningstype<Double>("Nedre inntektsterskel") { verdiAv(nedreTerskelFaktor) * verdiAv(grunnbeløp) }
        val øvreTerskelInntekt =
            Opplysningstype<Double>("Nedre inntektsterskel") { verdiAv(øvreTerskelFaktor) * verdiAv(grunnbeløp) }
        val inntektSiste12mnd = Opplysningstype<Double>("Inntekt siste 12 måneder") { 199858.0 }

        val nedreTerskelInntektTest =
            Opplysningstype<Double>("Nedre inntektsterskel", regel = Multiplikasjon(nedreTerskelFaktor, grunnbeløp))

        nedreTerskelInntektTest.kanUtledes(opplysninger)
        val faktum = nedreTerskelInntektTest.utled(opplysninger)
        assertTrue(faktum.er(nedreTerskelInntektTest))

        // Finn alle opplysninger som ikke kan utledes (har ikke andre avhengigheter) og mangler
        val trenger =
            vilkårMinsteinntekt.utledesAv
                .filter { opplysninger.any { opplysning -> opplysning.er(it) } } // Kast alle som er kjent

        val virkningsdato = Opplysningstype<LocalDate>("Virkningsdato") {}

        val inntektOverNedreTerskel =
            Opplysningstype(
                "Inntekt over nedre terskel",
                vilkårMinsteinntekt,
            ) { verdiAv(inntektSiste12mnd) størreEnn verdiAv(nedreTerskelInntekt) }
        val inntektOverØvreTerskel =
            Opplysningstype(
                "Inntekt over øvre terskel",
                vilkårMinsteinntekt,
            ) { verdiAv(inntektSiste12mnd) størreEnn verdiAv(øvreTerskelInntekt) }

        with(regelmotor) {
            utledning(nedreTerskelFaktor) { behov("nedreTerskelFaktor", listOf(virkningsdato)) }
            utledning(nedreTerskelInntekt) { verdiAv(nedreTerskelFaktor) * verdiAv(grunnbeløp) }
            utledning(øvreTerskelInntekt) { verdiAv(øvreTerskelFaktor) * verdiAv(grunnbeløp) }
            vurdering(inntektOverNedreTerskel) { verdiAv(inntektSiste12mnd) størreEnn verdiAv(nedreTerskelInntekt) }
            vurdering(inntektOverØvreTerskel) { verdiAv(inntektSiste12mnd) størreEnn verdiAv(øvreTerskelInntekt) }
            vurdering(vilkårMinsteinntekt) { verdiAv(inntektOverNedreTerskel) eller verdiAv(inntektOverØvreTerskel) }
        }

        val oppfylt: Boolean = opplysninger.erKjent(allevilkår) && opplysninger.erOppfylt(allevilkår)
        val manglende: List<Opplysningstype> = opplysninger.utred(allevilkår)

        // manglende = listOf(nedreTerskelFaktor, øvreTerskelFaktor, grunnbeløp, inntektSiste12mnd)
        opplysninger.leggTil(Faktum(nedreTerskelFaktor, 1.5))
        opplysninger.leggTil(Faktum(øvreTerskelFaktor, 3.0))
        opplysninger.leggTil(Faktum(grunnbeløp, 99858))
        opplysninger.leggTil(Faktum(inntektSiste12mnd, 199858))

        /*
        Hva skal starte ting?
        Hvordan kommunisere at vi mangler faktum? - Mangler funksjon for utledning?
        Slå sammen opplysningstype og utledning/regel/vurdering
        Datatyper(generics?) på opplysningstyper
         */

        Assertions.assertTrue(opplysninger.find { it.er(vilkårMinsteinntekt) }!!.verdi as Boolean)
        Assertions.assertTrue(
            allevilkår.bestårAv().all { vilkår -> opplysninger.find { it.er(vilkår) }!!.verdi as Boolean },
        )
        Assertions.assertTrue(opplysninger.filter { it.er(allevilkår) }.all { it.verdi as Boolean })*/
    }
}

/*
/*opplysninger.add(Faktum(fødselsdato, LocalDate.of(1980, 1, 1)))
opplysninger.add(Faktum(aldersgrense, 67))
opplysninger.add(Faktum(virkningsdato, LocalDate.now()))*/
val aldersgrense = Opplysningstype("Aldersgrense") // 67 år
val fødselsdato = Opplysningstype("Fødselsdato")
val sisteGrensedato = Opplysningstype("Siste mulige dato for innvilgelse")
val virkningsdato = Opplysningstype("Virkningsdato")

utledning(sisteGrensedato) { sisteDagIMåneden(fødselsdato + aldersgrense) }
vurdering(vilkårMaksalder) { virkningsdato mindreEnn sisteGrensedato }

vurdering(vilkår) { alle(vilkårMinsteinntekt, vilkårMaksalder) }
vurdering(rett) { vilkår }*/
