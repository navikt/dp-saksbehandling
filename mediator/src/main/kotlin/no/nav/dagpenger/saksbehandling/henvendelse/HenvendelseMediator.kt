package no.nav.dagpenger.saksbehandling.henvendelse

import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.HenvendelseMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

sealed class HåndterHenvendelseResultat {
    data class HåndtertHenvendelse(val sakId: UUID) : HåndterHenvendelseResultat()

    object UhåndtertHenvendelse : HåndterHenvendelseResultat()
}

class HenvendelseMediator(private val sakMediator: SakMediator, private val oppgaveMediator: OppgaveMediator) {
    fun taImotHenvendelse(hendelse: HenvendelseMottattHendelse): HåndterHenvendelseResultat {
        // TODO: Persister henvendelse og eventuell ytterligere håndtering av aktuell henvendelse

        if (hendelse.kategori == Kategori.ETTERSENDING && hendelse.søknadId != null) {
            val søknadId: UUID = hendelse.søknadId!!
            if (oppgaveMediator.skalEttersendingTilSøknadVarsles(ident = hendelse.ident, søknadId = søknadId)) {
                // TODO opprett henvendelse
            }
        }

        val sisteSakId = sakMediator.finnSisteSakId(hendelse.ident)

        return when (sisteSakId) {
            null -> {
                HåndterHenvendelseResultat.UhåndtertHenvendelse
            }

            else -> {
                HåndterHenvendelseResultat.HåndtertHenvendelse(sisteSakId)
            }
        }
    }
}

// Gammel kode:

//        when (kategori) {
//            Kategori.KLAGE -> {
//                val sisteSakId = sakMediator.finnSisteSakId(ident)
//                when (sisteSakId != null) {
//                    true -> {
//                        klageMediator.opprettKlage(
//                            KlageMottattHendelse(
//                                ident = ident,
//                                opprettet = registrertDato,
//                                journalpostId = journalpostId,
//                                sakId = sisteSakId,
//                                utførtAv = Applikasjon("dp-saksbehandling"),
//                            ),
//                        )
//                        packet.lagLøsning(håndtert = true, sakId = sisteSakId)
//                    }
//
//                    false -> packet.lagLøsning(håndtert = false)
//                }
//            }
//
//            Kategori.ANKE -> {
//                val sisteSakId = sakMediator.finnSisteSakId(ident)
//                when (sisteSakId != null) {
//                    true -> {
//                        // TODO Skal denne sendes rett til KA eller opprett henvendelse og ta det derfra?
//                        packet.lagLøsning(håndtert = true, sakId = sisteSakId)
//                    }
//
//                    false -> packet.lagLøsning(håndtert = false)
//                }
//            }
//
//            Kategori.ETTERSENDING -> {
//                if (!packet["søknadId"].isMissingNode && !packet["søknadId"].isNull) {
//                    val søknadId = packet["søknadId"].asUUID()
//                    if (oppgaveMediator.skalEttersendingTilSøknadVarsles(ident = ident, søknadId = søknadId)) {
//                        // TODO opprett henvendelse
//                    }
//                }
//                val sisteSakId = sakMediator.finnSisteSakId(ident)
//                when (sisteSakId != null) {
//                    // TODO skal vi alltid svare med siste sak og fikse journalføring i ettertid? Høna og egget...
//                    true -> packet.lagLøsning(håndtert = true, sakId = sisteSakId)
//                    false -> packet.lagLøsning(håndtert = false)
//                }
//            }
//
//            Kategori.UTDANNING -> {
//                val sisteSakId = sakMediator.finnSisteSakId(ident)
//                when (sisteSakId != null) {
//                    true -> packet.lagLøsning(håndtert = true, sakId = sisteSakId)
//                    false -> packet.lagLøsning(håndtert = false)
//                }
//            }
//
//            Kategori.ETABLERING -> {
//                val sisteSakId = sakMediator.finnSisteSakId(ident)
//                when (sisteSakId != null) {
//                    true -> packet.lagLøsning(håndtert = true, sakId = sisteSakId)
//                    false -> packet.lagLøsning(håndtert = false)
//                }
//            }
//
//            Kategori.GENERELL -> {
//                val sisteSakId = sakMediator.finnSisteSakId(ident)
//                when (sisteSakId != null) {
//                    true -> packet.lagLøsning(håndtert = true, sakId = sisteSakId)
//                    false -> packet.lagLøsning(håndtert = false)
//                }
//            }
//
//            Kategori.UKJENT_SKJEMA_KODE -> {
//                val sisteSakId = sakMediator.finnSisteSakId(ident)
//                when (sisteSakId != null) {
//                    true -> packet.lagLøsning(håndtert = true, sakId = sisteSakId)
//                    false -> packet.lagLøsning(håndtert = false)
//                }
//            }
//
//            else -> packet.lagLøsning(false)
//        }
