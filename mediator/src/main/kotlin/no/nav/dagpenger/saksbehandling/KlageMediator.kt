package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageOppgave
import java.time.LocalDate
import java.util.UUID

class KlageMediator(
    private val klageRepository: KlageRepository = InmemoryKlageRepository,
) {
    fun hentKlage(klageId: UUID): KlageBehandling {
        return klageRepository.hentKlage(klageId)
    }

    fun opprettKlage(klageMottattHendelse: KlageMottattHendelse): KlageOppgave {
        val oppgave =
            KlageOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = klageMottattHendelse.opprettet,
                klageBehandling =
                    KlageBehandling(
                        person =
                            Person(
                                id = TODO(),
                                ident = TODO(),
                                skjermesSomEgneAnsatte = TODO(),
                                adressebeskyttelseGradering = TODO(),
                            ),
                        opplysninger = TODO(),
                        steg = TODO(),
                    ),
            )
    }

    fun oppdaterKlageOpplysning(
        klageId: UUID,
        opplysningId: UUID,
        verdi: OpplysningerVerdi,
    ) {
        klageRepository.hentKlage(klageId).let { klage ->
            when (verdi) {
                is OpplysningerVerdi.Tekst -> klage.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.TekstListe -> klage.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.Dato -> klage.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.Boolsk -> klage.svar(opplysningId, verdi.value)
            }
        }
    }
}

sealed class OpplysningerVerdi {
    data class Tekst(val value: String) : OpplysningerVerdi()

    data class TekstListe(val value: List<String> = emptyList()) : OpplysningerVerdi(), List<String> by value {
        constructor(vararg values: String) : this(values.toList())
    }

    data class Dato(val value: LocalDate) : OpplysningerVerdi()

    data class Boolsk(val value: Boolean) : OpplysningerVerdi()

    companion object
}
