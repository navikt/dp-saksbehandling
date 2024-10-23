package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.AdressebeskyttelseTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.BeslutterRolleTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.EgneAnsatteTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.FerdigstillOppgaveTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.IngenTilgangTilOppgaveException
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.OppgaveTilgangskontroll
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import java.util.UUID

private val logger = KotlinLogging.logger {}

class SecureOppgaveMediator(
    private val oppgaveMediator: OppgaveMediator,
    private val adressebeskyttelseTilgangskontroll: AdressebeskyttelseTilgangskontroll =
        AdressebeskyttelseTilgangskontroll(
            strengtFortroligGruppe = Configuration.strengtFortroligADGruppe,
            strengtFortroligUtlandGruppe = Configuration.strengtFortroligUtlandADGruppe,
            fortroligGruppe = Configuration.fortroligADGruppe,
            adressebeskyttelseGraderingFun = { oppgaveId -> TODO() },
        ),
    private val beslutterTilgangskontroll: BeslutterRolleTilgangskontroll = BeslutterRolleTilgangskontroll,
    private val egneAnsatteTilgangskontroll: EgneAnsatteTilgangskontroll =
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf(Configuration.egneAnsatteADGruppe),
            skjermesSomEgneAnsatteFun = { oppgaveId -> TODO() },
        ),
    private val ferdigstillOppgaveTilgangskontroll: FerdigstillOppgaveTilgangskontroll =
        FerdigstillOppgaveTilgangskontroll(
            oppgaveFunc = { oppgaveId -> TODO() },
        ),
) {
    // TODO
    fun tildelNesteOppgaveTil(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        queryString: String,
    ): Oppgave? =
        oppgaveMediator.tildelOgHentNesteOppgave(
            nesteOppgaveHendelse = nesteOppgaveHendelse,
            queryString = queryString,
        )

    // DONE, krever ikke sikkerhet, da de bare lister opp oppgavene
    fun finnOppgaverFor(ident: String): List<Oppgave> {
        return oppgaveMediator.finnOppgaverFor(ident)
    }

    // DONE, krever ikke sikkerhet, da de bare lister opp oppgavene
    fun søk(søkefilter: Søkefilter): List<Oppgave> {
        return oppgaveMediator.søk(søkefilter)
    }

    // DONE
    fun hentOppgave(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return sjekkTilgang(
            kontroller = listOf(egneAnsatteTilgangskontroll, adressebeskyttelseTilgangskontroll),
            oppgaveId = oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.hentOppgave(oppgaveId, saksbehandler)
        }
    }

    // DONE
    fun tildelOppgave(
        oppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return sjekkTilgang(
            kontroller = listOf(egneAnsatteTilgangskontroll, adressebeskyttelseTilgangskontroll),
            oppgaveId = oppgaveAnsvarHendelse.oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.tildelOppgave(oppgaveAnsvarHendelse)
        }
    }

    // DONE
    fun utsettOppgave(
        utsettOppgaveHendelse: UtsettOppgaveHendelse,
        saksbehandler: Saksbehandler,
    ) {
        return sjekkTilgang(
            kontroller = listOf(egneAnsatteTilgangskontroll, adressebeskyttelseTilgangskontroll),
            oppgaveId = utsettOppgaveHendelse.oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.utsettOppgave(utsettOppgaveHendelse)
        }
    }

    // DONE - Man skal alltid kunne gi fra seg en oppgave, uansett hvilke rettigheter
    fun fristillOppgave(oppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse) {
        oppgaveMediator.fristillOppgave(oppgaveAnsvarHendelse)
    }

    // DONE
    fun sendTilKontroll(
        klarTilKontrollHendelse: KlarTilKontrollHendelse,
        saksbehandler: Saksbehandler,
    ) {
        return sjekkTilgang(
            kontroller = listOf(egneAnsatteTilgangskontroll, adressebeskyttelseTilgangskontroll),
            oppgaveId = klarTilKontrollHendelse.oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.sendTilKontroll(klarTilKontrollHendelse)
        }
    }

    // DONE
    fun tildelTotrinnskontroll(
        toTrinnskontrollHendelse: ToTrinnskontrollHendelse,
        saksbehandler: Saksbehandler,
    ) {
        return sjekkTilgang(
            kontroller =
                listOf(
                    egneAnsatteTilgangskontroll,
                    adressebeskyttelseTilgangskontroll,
                    beslutterTilgangskontroll,
                ),
            oppgaveId = toTrinnskontrollHendelse.oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.tildelTotrinnskontroll(toTrinnskontrollHendelse)
        }
    }

    // DONE
    fun ferdigstillOppgave(
        godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        return sjekkTilgang(
            kontroller =
                listOf(
                    egneAnsatteTilgangskontroll,
                    adressebeskyttelseTilgangskontroll,
                    ferdigstillOppgaveTilgangskontroll,
                ),
            oppgaveId = godkjentBehandlingHendelse.oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.ferdigstillOppgave(godkjentBehandlingHendelse, saksbehandlerToken)
        }
    }

    // DONE
    fun ferdigstillOppgave(
        godkjentBehandlingHendelse: GodkjennBehandlingMedBrevIArena,
        saksbehandler: Saksbehandler,
        saksbehandlerToken: String,
    ) {
        return sjekkTilgang(
            kontroller =
                listOf(
                    egneAnsatteTilgangskontroll,
                    adressebeskyttelseTilgangskontroll,
                    ferdigstillOppgaveTilgangskontroll,
                ),
            oppgaveId = godkjentBehandlingHendelse.oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.ferdigstillOppgave(godkjentBehandlingHendelse, saksbehandlerToken)
        }
    }

    // DONE
    fun hentOppgaveIdFor(behandlingId: UUID): UUID? {
        return oppgaveMediator.hentOppgaveIdFor(behandlingId)
    }

    private fun <T> sjekkTilgang(
        kontroller: List<OppgaveTilgangskontroll>,
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
        block: (oppgaveId: UUID) -> T,
    ): T {
        val feilenedeValideringer =
            kontroller.firstOrNull {
                it.harTilgang(oppgaveId, saksbehandler) == false
            }
        return when (feilenedeValideringer) {
            null -> {
                block(oppgaveId)
            }

            else -> {
                logger.info {
                    "Saksbehandler ${saksbehandler.navIdent} har IKKE tilgang til oppgave med id $oppgaveId." +
                        " Tilganger: ${saksbehandler.grupper}"
                }
                throw IngenTilgangTilOppgaveException(
                    feilenedeValideringer.feilmelding(oppgaveId, saksbehandler),
                    feilenedeValideringer.feilType(oppgaveId, saksbehandler),
                )
            }
        }
    }
}
