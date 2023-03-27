package no.nav.dagpenger.behandling

object Hubba {
    fun bubba(): Behandling =
        behandling {
            val virkingstidspunkt = steg {
                fastsettelse("Virkningstidspunkt")
            }

            steg {
                vilkår("Vilkår for søknaden omfattet av norske regler")
            }

            steg {
                vilkår("Vilkår for medlem i folketrygden")
            }

            steg {
                vilkår("Vilkår for oppholder seg i Norge") {
                    avhengerAvFastsettelse("Bostedsadresse")
                }
            }

            steg {
                vilkår("Vilkår for tapt arbeidsinntekt") {
                    avhengerAv(virkingstidspunkt)
                }
            }

            steg {
                vilkår("Vilkår for krav til minsteinntekt") {
                    avhengerAv(virkingstidspunkt)
                    avhengerAvFastsettelse("Verneplikt")
                }
            }

            steg {
                vilkår("Vilkår for reell arbeidssøker") {
                    avhengerAvVilkår("Vilkår: Er bruker minst 50% arbeidsfør?")
                    avhengerAvVilkår("Vilkår: Er bruker villig og i stand til å ta ethvert arbeide som er lønnet etter tariff og sedvane?")
                    avhengerAvVilkår("Vilkår: Er bruker villig og i stand til å ta arbeid i hele Norge?")
                    avhengerAvVilkår("Vilkår: Er bruker villig og i stand til å ta arbeid både på heltid og deltid?")
                }
            }

            steg {
                vilkår("Vilkår for alder") {
                    avhengerAvFastsettelse("alder") {
                        avhengerAvFastsettelse("fødselsdato")
                    }
                }
            }

            steg {
                vilkår("Vilkår: Har brukeren andre folketrygd-ytelser?")
            }

            steg {
                vilkår("Har du barn eller noe") {
                    avhengerAvFastsettelse("Alle barna som er viktig for at du skal få penger av oss")
                }
            }
        }
}
