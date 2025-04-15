package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageOppgave
import java.time.LocalDate
import java.util.UUID

class KlageMediator(
    private val klageRepository: KlageRepository = InmemoryKlageRepository,
    private val personRepository: PersonRepository,
    private val oppslag: Oppslag,
) {
    fun hentKlageOppgave(klageOppgaveId: UUID): KlageOppgave {
        return klageRepository.hentKlageOppgave(klageOppgaveId)
    }

    fun opprettKlage(klageMottattHendelse: KlageMottattHendelse) {
        val person =
            personRepository.finnPerson(klageMottattHendelse.ident) ?: runBlocking {
                oppslag.hentPersonMedSkjermingOgGradering(klageMottattHendelse.ident)
            }

        val oppgave =
            KlageOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = klageMottattHendelse.opprettet,
                journalpostId = klageMottattHendelse.journalpostId,
                klageBehandling =
                    KlageBehandling(
                        person = person,
                    ),
            )

        klageRepository.lagre(oppgave)
    }

    fun oppdaterKlageOpplysning(
        klageOppgaveId: UUID,
        opplysningId: UUID,
        verdi: OpplysningerVerdi,
    ) {
        klageRepository.hentKlageOppgave(klageOppgaveId).klageBehandling.let { klage ->
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
