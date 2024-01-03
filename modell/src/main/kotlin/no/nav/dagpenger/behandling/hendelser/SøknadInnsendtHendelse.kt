package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Rolle.Beslutter
import no.nav.dagpenger.behandling.dsl.BehandlingDSL
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.oppgave.Oppgave
import java.time.LocalDate
import java.util.UUID

class SøknadInnsendtHendelse(
    meldingsreferanseId: UUID = UUID.randomUUID(),
    private val søknadId: UUID,
    private val journalpostId: String,
    ident: String,
    val innsendtDato: LocalDate,
) : PersonHendelse(meldingsreferanseId, ident) {
    fun søknadId() = søknadId

    fun journalpostId() = journalpostId

    override fun kontekst() =
        mapOf(
            "søknadId" to søknadId.toString(),
            "journalpostId" to journalpostId,
        )

    fun oppgave(person: Person): Oppgave {
        val behandling = behandling(person, this, person.hentGjeldendeSak(), behandlingDSL())

        return Oppgave(UUID.randomUUID(), behandling)
    }

    private fun behandlingDSL(): BehandlingDSL.() -> Unit =
        {
            val virkningsdato =
                steg {
                    fastsettelse<LocalDate>("Virkningsdato")
                }
            val rettighetstype =
                steg {
                    fastsettelse<String>("Rettighetstype")
                }
            val fastsattVanligArbeidstid =
                steg {
                    fastsettelse<Double>("Fastsatt vanlig arbeidstid") {
                        avhengerAv(virkningsdato)
                    }
                }
            val vilkår =
                steg {
                    vilkår("Oppfyller kravene til dagpenger") {
                        avhengerAv(virkningsdato)
                    }
                }
            val grunnlag =
                steg {
                    fastsettelse<Int>("Grunnlag") {
                        avhengerAv(virkningsdato)
                    }
                }
            val sats =
                steg {
                    fastsettelse<Int>("Dagsats") {
                        avhengerAv(grunnlag)
                    }
                }
            val periode =
                steg {
                    fastsettelse<Int>("Periode") {
                        avhengerAv(grunnlag)
                    }
                }
            val forslagTilVedtak =
                steg {
                    prosess("Forslag til vedtak") {
                        avhengerAv(vilkår)
                        avhengerAv(grunnlag)
                        avhengerAv(rettighetstype)
                        avhengerAv(fastsattVanligArbeidstid)
                        avhengerAv(sats)
                        avhengerAv(periode)
                    }
                }
            steg {
                prosess("Fatt vedtak", rolle = Beslutter) {
                    avhengerAv(forslagTilVedtak)
                }
            }
        }

    override fun hashCode(): Int = søknadId.hashCode() + journalpostId.hashCode() + super.hashCode()

    override fun equals(other: Any?): Boolean {
        return other is SøknadInnsendtHendelse && other.søknadId == søknadId && super.equals(other)
    }

    override fun toString(): String {
        return "SøknadInnsendtHendelse(søknadId=$søknadId, journalpostId='$journalpostId')"
    }
}
