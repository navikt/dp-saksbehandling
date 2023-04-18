package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import java.time.LocalDate
import java.util.UUID

class SøknadInnsendtHendelse(private val søknadId: UUID, private val journalpostId: String, ident: String) :
    Hendelse(ident) {
    fun søknadId() = søknadId
    fun journalpostId() = journalpostId
    fun lagBehandling(): Behandling {
        return behandling(Person(ident())) {
            val virkningsdato = steg {
                fastsettelse<LocalDate>("Virkningsdato")
            }

            steg {
                fastsettelse<String>("Rettighetstype")
            }
            steg {
                fastsettelse<Int>("Fastsatt vanlig arbeidstid") {
                    avhengerAv(virkningsdato)
                }
            }
            val grunnlag = steg {
                fastsettelse<Int>("Grunnlag") {
                    avhengerAv(virkningsdato)
                }
            }
            steg {
                fastsettelse<Int>("Dagsats") {
                    avhengerAv(grunnlag)
                }
            }
            steg {
                fastsettelse<Int>("Periode") {
                    avhengerAv(grunnlag)
                }
            }
            steg {
                vilkår("Oppfyller kravene til dagpenger") {
                    avhengerAv(virkningsdato)
                }
            }
        }
    }
}
