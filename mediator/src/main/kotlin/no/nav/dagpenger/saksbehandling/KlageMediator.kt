package no.nav.dagpenger.saksbehandling

import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.klage.InmemoryKlageRepository
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillKlageOppgave
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import java.time.LocalDate
import java.util.UUID

class KlageMediator(
    private val klageRepository: KlageRepository = InmemoryKlageRepository,
    private val personRepository: PersonRepository,
    private val oppslag: Oppslag,
    private val utsendingMediator: UtsendingMediator,
) {
    fun hentKlageBehandling(behandlingId: UUID): KlageBehandling = klageRepository.hentKlageBehandling(behandlingId)

    fun opprettKlage(klageMottattHendelse: KlageMottattHendelse) {
        val person =
            personRepository.finnPerson(klageMottattHendelse.ident) ?: runBlocking {
                oppslag.hentPersonMedSkjermingOgGradering(klageMottattHendelse.ident)
            }

        val klageBehandling =
            KlageBehandling(
                behandlingId = UUIDv7.ny(),
            )

        val oppgave =
            Oppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = klageMottattHendelse.opprettet,
                tilstand = Oppgave.KlarTilBehandling,
                behandling =
                    Behandling(
                        behandlingId = klageBehandling.behandlingId,
                        person = person,
                        opprettet = klageMottattHendelse.opprettet,
                        hendelse = klageMottattHendelse,
                        type = BehandlingType.KLAGE,
                    ),
            )

        klageRepository.lagreOppgaveOgKlage(oppgave, klageBehandling)
    }

    fun oppdaterKlageOpplysning(
        behandlingId: UUID,
        opplysningId: UUID,
        verdi: OpplysningerVerdi,
    ) {
        klageRepository.hentKlageBehandling(behandlingId).let { klageBehandling ->
            when (verdi) {
                is OpplysningerVerdi.Tekst -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.TekstListe -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.Dato -> klageBehandling.svar(opplysningId, verdi.value)
                is OpplysningerVerdi.Boolsk -> klageBehandling.svar(opplysningId, verdi.value)
            }
        }
    }

    fun ferdigstill(hendelse: FerdigstillKlageOppgave) {
        val html = "må hente html"

        val oppgave =
            klageRepository.hentOppgaveFor(hendelse.behandlingId).also { oppgave ->
                oppgave.ferdigstill(
                    GodkjentBehandlingHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        meldingOmVedtak = "todo",
                        utførtAv = hendelse.utførtAv,
                    ),
                )
            }

        val klageBehandling =
            klageRepository.hentKlageBehandling(hendelse.behandlingId).also { klageBehandling ->
                klageBehandling.ferdigstill()
            }

        val startUtsendingHendelse =
            StartUtsendingHendelse(
                oppgaveId = oppgave.oppgaveId,
                sak =
                    Sak(
                        id = UUIDv7.ny().toString(),
                        kontekst = "Dagpenger",
                    ),
                behandlingId = klageBehandling.behandlingId,
                ident = oppgave.behandling.person.ident,
            )

        utsendingMediator.opprettUtsending(
            oppgaveId = oppgave.oppgaveId,
            brev = html,
            ident = oppgave.behandling.person.ident,
        )
        klageRepository.lagreOppgaveOgKlage(oppgave, klageBehandling)
        utsendingMediator.mottaStartUtsending(
            startUtsendingHendelse,
        )
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
