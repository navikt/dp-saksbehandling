package no.nav.dagpenger.saksbehandling.db.klage

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import java.util.UUID

interface KlageRepository {
    fun hentKlage(klageId: UUID): KlageBehandling

    fun hentKlager(): List<KlageBehandling>

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
        mutableMapOf<UUID, KlageBehandling>().also { klager ->
            testKlageId1.let { id ->
                klager[id] =
                    KlageBehandling(
                        id = id,
                        person = personer[id] ?: throw IllegalStateException("Fant ikke person med id $id"),
                    )
            }
        }

    override fun hentKlage(klageId: UUID): KlageBehandling =
        klager[klageId] ?: throw KlageRepository.KlageIkkeFunnet("Fant ikke klage med id $klageId")

    override fun hentKlager(): List<KlageBehandling> = klager.values.toList()
}
