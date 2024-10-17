package no.nav.dagpenger.saksbehandling

import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.AdressebeskyttelseTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.BeslutterRolleTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.EgneAnsatteTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.FerdigstillOppgaveTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.IngenTilgangTilOppgaveException
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.OppgaveBehandlerTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.OppgaveTilgangskontroll
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
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
            adressebeskyttelseGraderingFun = oppgaveMediator::adresseGraderingForPerson,
        ),
    private val beslutterTilgangskontroll: BeslutterRolleTilgangskontroll = BeslutterRolleTilgangskontroll,
    private val egneAnsatteTilgangskontroll: EgneAnsatteTilgangskontroll =
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf(Configuration.egneAnsatteADGruppe),
            skjermesSomEgneAnsatteFun = oppgaveMediator::personSkjermesSomEgneAnsatte,
        ),
    private val behandlerTilgangskontroll: OppgaveBehandlerTilgangskontroll =
        OppgaveBehandlerTilgangskontroll(
            behandlerFunc = oppgaveMediator::behandlerForOppgave,
        ),
    private val ferdigstillOppgaveTilgangskontroll: FerdigstillOppgaveTilgangskontroll =
        FerdigstillOppgaveTilgangskontroll(
            oppgaveFunc = oppgaveMediator::hentOppgave,
        ),
) {
    fun finnOppgaverFor(ident: String): List<Oppgave> {
        return oppgaveMediator.finnOppgaverFor(ident)
    }

    fun søk(søkefilter: Søkefilter): List<Oppgave> {
        return oppgaveMediator.søk(søkefilter)
    }

    fun tildelNesteOppgaveTil(
        nesteOppgaveHendelse: NesteOppgaveHendelse,
        queryString: String,
    ): Oppgave? =
        oppgaveMediator.tildelOgHentNesteOppgave(
            nesteOppgaveHendelse = nesteOppgaveHendelse,
            filter =
                TildelNesteOppgaveFilter.fra(
                    queryString = queryString,
                    saksbehandlerTilgangEgneAnsatte = egneAnsatteTilgangskontroll.harTilgang(nesteOppgaveHendelse.utførtAv),
                    adresseBeskyttelseGradering = adressebeskyttelseTilgangskontroll.tilganger(nesteOppgaveHendelse.utførtAv),
                ),
        )

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

    fun hentOppgave(
        oppgaveId: UUID,
        saksbehandler: Saksbehandler,
    ): Oppgave {
        return sjekkTilgang(
            kontroller = listOf(egneAnsatteTilgangskontroll, adressebeskyttelseTilgangskontroll),
            oppgaveId = oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.hentOppgave(oppgaveId)
        }
    }

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

    fun fristillOppgave(oppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse) {
        oppgaveMediator.fristillOppgave(oppgaveAnsvarHendelse)
    }

    fun gjørKlarTilKontroll(
        klarTilKontrollHendelse: KlarTilKontrollHendelse,
        saksbehandler: Saksbehandler,
    ) {
        return sjekkTilgang(
            kontroller = listOf(egneAnsatteTilgangskontroll, adressebeskyttelseTilgangskontroll),
            oppgaveId = klarTilKontrollHendelse.oppgaveId,
            saksbehandler = saksbehandler,
        ) {
            oppgaveMediator.gjørKlarTilKontroll(klarTilKontrollHendelse)
        }
    }

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

    fun hentOppgaveIdFor(behandlingId: UUID): UUID? {
        return oppgaveMediator.hentOppgaveIdFor(behandlingId)
    }
}
