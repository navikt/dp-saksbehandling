package no.nav.dagpenger.saksbehandling.db.klage

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageOppgave
import java.time.LocalDateTime
import java.util.UUID

interface KlageRepository {
    fun hentKlageOppgave(klageId: UUID): KlageOppgave

    fun hentKlager(): List<KlageOppgave>

    fun lagre(klage: KlageOppgave)

    class KlageIkkeFunnet(message: String) : RuntimeException(message)
}

object InmemoryKlageRepository : KlageRepository {
    val testKlageId1 = UUID.fromString("01905da1-32bc-7f57-83df-61dcd3a20ea6")

    private val personer =
        mutableMapOf<UUID, Person>().also { personer ->
            UUID.fromString("01905da1-32bc-7f57-83df-61dcd3a20ea6").let { id ->
                personer[id] =
                    Person(
                        id = id,
                        ident = "06918097854",
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
                    )
            }
        }

    private val klager =
        mutableMapOf<UUID, KlageOppgave>().also { klager ->
            testKlageId1.let { id ->
                klager[id] =
                    KlageOppgave(
                        oppgaveId = id,
                        opprettet = LocalDateTime.now(),
                        klageBehandling =
                            KlageBehandling(
                                id = UUIDv7.ny(),
                                person = personer[id]!!,
                            ),
                    )
            }
        }

    override fun hentKlageOppgave(klageId: UUID): KlageOppgave =
        klager[klageId] ?: throw KlageRepository.KlageIkkeFunnet("Fant ikke klage med id $klageId")

    override fun hentKlager(): List<KlageOppgave> = klager.values.toList()

    override fun lagre(klage: KlageOppgave) {
        klager[klage.oppgaveId] = klage
        personer[klage.klageBehandling.person.id] = klage.klageBehandling.person
    }
}
