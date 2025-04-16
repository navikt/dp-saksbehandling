package no.nav.dagpenger.saksbehandling.db.klage

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import java.time.LocalDateTime
import java.util.UUID

interface KlageRepository {
    fun hentKlageOppgave(klageId: UUID): Oppgave

    fun hentKlager(): List<Oppgave>

    fun lagre(klage: Oppgave)

    fun lagre(klageBehandling: KlageBehandling)

    fun hentKlageBehandling(behandlingId: UUID): KlageBehandling

    class KlageIkkeFunnet(message: String) : RuntimeException(message)

    class KlageBehandlingIkkeFunnet(message: String) : RuntimeException(message)
}

object InmemoryKlageRepository : KlageRepository {
    val testKlageId1 = UUID.fromString("01905da1-32bc-7f57-83df-61dcd3a20ea6")

    val testPerson1 =
        Person(
            id = testKlageId1,
            ident = "06918097854",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )
    private val personer =
        mutableMapOf<UUID, Person>().also {
            it[testPerson1.id] = testPerson1
        }

    private val oppgaver =
        mutableMapOf<UUID, Oppgave>().also { klager ->
            testKlageId1.let { id ->
                klager[id] =
                    Oppgave(
                        oppgaveId = id,
                        opprettet = LocalDateTime.now(),
                        behandling =
                            Behandling(
                                behandlingId = id,
                                person = personer[id]!!,
                                opprettet = LocalDateTime.now(),
                                type = BehandlingType.KLAGE,
                            ),
                    )
            }
        }
    private val klageBehandlinger =
        mutableMapOf<UUID, KlageBehandling>().also {
            it[testKlageId1] =
                KlageBehandling(
                    behandlingId = testKlageId1,
                )
        }

    override fun hentKlageBehandling(behandlingId: UUID): KlageBehandling {
        return klageBehandlinger[behandlingId]
            ?: throw KlageRepository.KlageBehandlingIkkeFunnet("Fant ikke klagebehandling med id $behandlingId")
    }

    override fun hentKlageOppgave(klageId: UUID): Oppgave =
        oppgaver[klageId] ?: throw KlageRepository.KlageIkkeFunnet("Fant ikke oppgave med id $klageId")

    override fun hentKlager(): List<Oppgave> = oppgaver.values.toList()

    override fun lagre(oppgave: Oppgave) {
        oppgaver[oppgave.oppgaveId] = oppgave
        personer[oppgave.behandling.person.id] = oppgave.behandling.person
    }

    override fun lagre(klageBehandling: KlageBehandling) {
        klageBehandlinger[klageBehandling.behandlingId] = klageBehandling
    }
}
