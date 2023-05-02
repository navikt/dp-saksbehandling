package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.SpesifikkKontekst
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.prosess.Arbeidsprosesser
import java.time.LocalDate
import java.util.UUID

class SøknadInnsendtHendelse(private val søknadId: UUID, private val journalpostId: String, ident: String) :
    Hendelse(ident) {
    fun søknadId() = søknadId
    fun journalpostId() = journalpostId

    override fun toSpesifikkKontekst() = SpesifikkKontekst(
        this.javaClass.simpleName,
        mapOf(
            "søknadId" to søknadId.toString(),
            "journalpostId" to journalpostId,
        ),
    )

    fun oppgave() = Oppgave(
        behandling,
        Arbeidsprosesser.totrinnsprosess(behandling).apply { start("TilBehandling") },
    )

    val behandling: Behandling = behandling(Person(ident()), this) {
        val virkningsdato = steg {
            fastsettelse<LocalDate>("Virkningsdato")
        }

        steg {
            fastsettelse<String>("Rettighetstype")
        }
        steg {
            fastsettelse<Double>("Fastsatt vanlig arbeidstid") {
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
