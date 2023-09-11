package no.nav.dagpenger.behandling.hendelser

import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Rolle.Beslutter
import no.nav.dagpenger.behandling.dsl.BehandlingDSL
import no.nav.dagpenger.behandling.dsl.BehandlingDSL.Companion.behandling
import no.nav.dagpenger.behandling.oppgave.Oppgave
import java.time.LocalDate
import java.util.UUID

class VedtakStansetHendelse(
    ident: String,
    val oppgaveId: UUID = UUID.randomUUID(),
) : PersonHendelse(UUID.randomUUID(), ident) {

    fun oppgave(person: Person): Oppgave {
        val behandling = behandling(
            person = person,
            hendelse = this,
            sak = person.hentGjeldendeSak(),
            block = behandlingDSLForStans(),
        )

        return Oppgave(oppgaveId, behandling)
    }

    private fun behandlingDSLForStans(): BehandlingDSL.() -> Unit = {
        val virkningsdato = steg {
            fastsettelse<LocalDate>("Virkningsdato")
        }
        val forslagTilVedtak = steg {
            prosess("Forslag til vedtak") {
                avhengerAv(virkningsdato)
            }
        }
        steg {
            prosess("Fatt vedtak") {
                rolle = Beslutter
                avhengerAv(forslagTilVedtak)
            }
        }
    }
}
