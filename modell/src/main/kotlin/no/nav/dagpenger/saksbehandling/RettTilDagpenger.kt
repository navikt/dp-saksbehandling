package no.nav.dagpenger.saksbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.FerdigstillBehandling.BESLUTT
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.KontrollertBrev.IKKE_RELEVANT
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.KontrollertBrev.JA
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.KontrollertBrev.NEI
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.AVBRUTT
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.RettTilDagpenger.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.hendelser.AnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.EndreMeldingOmVedtakKildeHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelseUtenMeldingOmVedtak
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.LagreBrevKvitteringHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SlettNotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.tilgangsstyring.ManglendeTilgang
import no.nav.dagpenger.saksbehandling.tilgangsstyring.SaksbehandlerErIkkeEier
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

data class RettTilDagpenger private constructor(
    override val oppgaveId: UUID,
    override val opprettet: LocalDateTime,
    override var behandlerIdent: String? = null,
    override val emneknagger: MutableSet<String>,
    override var tilstand: Tilstand = KlarTilBehandling,
    override var utsattTil: LocalDate? = null,
    override val tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
    override val person: Person,
    override val behandling: Behandling,
    override var meldingOmVedtak: MeldingOmVedtak,
) : Oppgave() {
    constructor(
        oppgaveId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: LocalDateTime,
        tilstand: Tilstand = KlarTilBehandling,
        behandlerIdent: String? = null,
        tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
        person: Person,
        behandling: Behandling,
        meldingOmVedtak: MeldingOmVedtak,
    ) : this(
        oppgaveId = oppgaveId,
        behandlerIdent = behandlerIdent,
        opprettet = opprettet,
        emneknagger = emneknagger.toMutableSet(),
        tilstand = tilstand,
        tilstandslogg = tilstandslogg,
        person = person,
        behandling = behandling,
        meldingOmVedtak = meldingOmVedtak,
    )

    companion object {
        internal const val RETUR_FRA_KONTROLL = "Retur fra kontroll"
        internal const val TIDLIGERE_KONTROLLERT = "Tidligere kontrollert"
        internal val kontrollEmneknagger: Set<String> = setOf(RETUR_FRA_KONTROLL, TIDLIGERE_KONTROLLERT)
        internal val påVentEmneknagger: Set<String> =
            Emneknagg.PåVent.entries
                .map { påVentÅrsaker ->
                    påVentÅrsaker.visningsnavn
                }.toSet()

        fun rehydrer(
            oppgaveId: UUID,
            behandlerIdent: String?,
            opprettet: LocalDateTime,
            emneknagger: Set<String>,
            tilstand: Tilstand,
            utsattTil: LocalDate?,
            tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
            person: Person,
            behandling: Behandling,
            meldingOmVedtak: MeldingOmVedtak,
        ): RettTilDagpenger =
            RettTilDagpenger(
                oppgaveId = oppgaveId,
                opprettet = opprettet,
                behandlerIdent = behandlerIdent,
                emneknagger = emneknagger.toMutableSet(),
                tilstand = tilstand,
                utsattTil = utsattTil,
                tilstandslogg = tilstandslogg,
                person = person,
                behandling = behandling,
                meldingOmVedtak = meldingOmVedtak,
            )

        private fun requireEierskapTilOppgave(
            oppgave: RettTilDagpenger,
            saksbehandler: Saksbehandler,
            hendelseNavn: String,
        ) {
            require(oppgave.erEierAvOppgave(saksbehandler)) {
                throw SaksbehandlerErIkkeEier(
                    "Ulovlig hendelse $hendelseNavn på oppgave i tilstand ${oppgave.tilstand.type} uten å eie oppgaven. " +
                        "Oppgave eies av ${oppgave.behandlerIdent} og ikke ${saksbehandler.navIdent}",
                )
            }
        }

        private fun requireBeslutterTilgang(
            saksbehandler: Saksbehandler,
            tilstandType: Type,
            hendelseNavn: String,
        ) {
            require(saksbehandler.tilganger.contains(BESLUTTER)) {
                throw Tilstand.ManglendeBeslutterTilgang("Kan ikke behandle $hendelseNavn i tilstand $tilstandType uten beslutter-tilgang")
            }
        }

        private fun requireBeslutterUlikSaksbehandler(
            oppgave: RettTilDagpenger,
            beslutter: Saksbehandler,
            hendelseNavn: String,
        ) {
            require(oppgave.sisteSaksbehandler() != beslutter.navIdent) {
                throw Tilstand.KanIkkeBeslutteEgenSaksbehandling(
                    "Ulovlig hendelse $hendelseNavn på oppgave i tilstand ${oppgave.tilstand.type}. " +
                        "Oppgave kan ikke behandles og kontrolleres av samme person. Saksbehandler på oppgaven er " +
                        "${oppgave.sisteSaksbehandler()} og kan derfor ikke kontrolleres av ${beslutter.navIdent}",
                )
            }
        }

        private fun requireKvittertGosysBrev(
            oppgave: RettTilDagpenger,
            hendelseNavn: String,
        ) {
            if (oppgave.meldingOmVedtak.kilde == GOSYS) {
                require(oppgave.meldingOmVedtak.kontrollertGosysBrev == JA) {
                    throw Tilstand.KreverKontrollAvGosysBrev(
                        "Brev i Gosys må være kontrollert av beslutter for å kunne behandle $hendelseNavn i " +
                            "tilstand ${oppgave.tilstand.type}. Brevkilde: ${oppgave.meldingOmVedtak.kilde}, " +
                            "KontrollertGosysBrev: ${oppgave.meldingOmVedtak.kontrollertGosysBrev}",
                    )
                }
            }
        }
    }

    fun emneknagger(): Set<String> = emneknagger.toSet()

    fun tilstandslogg(): OppgaveTilstandslogg = tilstandslogg

    fun personIdent() = person.ident

    fun tilstand() = this.tilstand

    fun meldingOmVedtakKilde() = this.meldingOmVedtak.kilde

    fun kontrollertBrev() = this.meldingOmVedtak.kontrollertGosysBrev

    fun egneAnsatteTilgangskontroll(saksbehandler: Saksbehandler) = this.person.egneAnsatteTilgangskontroll(saksbehandler)

    fun adressebeskyttelseTilgangskontroll(saksbehandler: Saksbehandler) = this.person.adressebeskyttelseTilgangskontroll(saksbehandler)

    fun utsattTil() = this.utsattTil

    fun oppgaveKlarTilBehandling(forslagTilVedtakHendelse: ForslagTilVedtakHendelse): Handling {
        val beholdEmneknagger = this.emneknagger().filter { it in kontrollEmneknagger + påVentEmneknagger }.toSet()
        this.emneknagger.clear()
        this.emneknagger.addAll(forslagTilVedtakHendelse.emneknagger)
        this.emneknagger.addAll(beholdEmneknagger)
        return tilstand.oppgaveKlarTilBehandling(this, forslagTilVedtakHendelse)
    }

    fun avbryt(avbrytOppgaveHendelse: AvbrytOppgaveHendelse) {
        adressebeskyttelseTilgangskontroll(avbrytOppgaveHendelse.utførtAv)
        egneAnsatteTilgangskontroll(avbrytOppgaveHendelse.utførtAv)
        this.emneknagger.add(avbrytOppgaveHendelse.årsak.visningsnavn)
        tilstand.avbryt(this, avbrytOppgaveHendelse)
    }

    fun ferdigstill(vedtakFattetHendelse: VedtakFattetHendelse): Handling = tilstand.ferdigstill(this, vedtakFattetHendelse)

    fun ferdigstill(godkjentBehandlingHendelse: GodkjentBehandlingHendelse): FerdigstillBehandling {
        adressebeskyttelseTilgangskontroll(godkjentBehandlingHendelse.utførtAv)
        egneAnsatteTilgangskontroll(godkjentBehandlingHendelse.utførtAv)
        return tilstand.ferdigstill(this, godkjentBehandlingHendelse)
    }

    fun ferdigstill(godkjentBehandlingHendelseUtenMeldingOmVedtak: GodkjentBehandlingHendelseUtenMeldingOmVedtak): FerdigstillBehandling {
        adressebeskyttelseTilgangskontroll(godkjentBehandlingHendelseUtenMeldingOmVedtak.utførtAv)
        egneAnsatteTilgangskontroll(godkjentBehandlingHendelseUtenMeldingOmVedtak.utførtAv)
        return tilstand.ferdigstill(this, godkjentBehandlingHendelseUtenMeldingOmVedtak)
    }

    fun ferdigstill(avbruttHendelse: AvbruttHendelse) {
        adressebeskyttelseTilgangskontroll(avbruttHendelse.utførtAv)
        egneAnsatteTilgangskontroll(avbruttHendelse.utførtAv)
        tilstand.ferdigstill(this, avbruttHendelse)
    }

    fun fjernAnsvar(fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse) {
        tilstand.fjernAnsvar(this, fjernOppgaveAnsvarHendelse)
    }

    fun tildel(settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse) {
        egneAnsatteTilgangskontroll(settOppgaveAnsvarHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(settOppgaveAnsvarHendelse.utførtAv)
        tilstand.tildel(this, settOppgaveAnsvarHendelse)
    }

    fun utsett(utsettOppgaveHendelse: UtsettOppgaveHendelse) {
        egneAnsatteTilgangskontroll(utsettOppgaveHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(utsettOppgaveHendelse.utførtAv)
        tilstand.utsett(this, utsettOppgaveHendelse)
    }

    fun sendTilKontroll(sendTilKontrollHendelse: SendTilKontrollHendelse) {
        egneAnsatteTilgangskontroll(sendTilKontrollHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(sendTilKontrollHendelse.utførtAv)
        tilstand.sendTilKontroll(this, sendTilKontrollHendelse)
    }

    fun avbryt(behandlingAvbruttHendelse: BehandlingAvbruttHendelse) {
        tilstand.avbryt(this, behandlingAvbruttHendelse)
    }

    fun endreMeldingOmVedtakKilde(endreMeldingOmVedtakKildeHendelse: EndreMeldingOmVedtakKildeHendelse) {
        egneAnsatteTilgangskontroll(endreMeldingOmVedtakKildeHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(endreMeldingOmVedtakKildeHendelse.utførtAv)
        tilstand.endreMeldingOmVedtakKilde(this, endreMeldingOmVedtakKildeHendelse)
    }

    fun lagreBrevKvittering(lagreBrevKvitteringHendelse: LagreBrevKvitteringHendelse) {
        egneAnsatteTilgangskontroll(lagreBrevKvitteringHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(lagreBrevKvitteringHendelse.utførtAv)
        tilstand.lagreBrevKvittering(this, lagreBrevKvitteringHendelse)
    }

    fun lagreNotat(notatHendelse: NotatHendelse) {
        egneAnsatteTilgangskontroll(notatHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(notatHendelse.utførtAv)
        tilstand.lagreNotat(this, notatHendelse)
    }

    fun slettNotat(slettNotatHendelse: SlettNotatHendelse) {
        egneAnsatteTilgangskontroll(slettNotatHendelse.utførtAv)
        adressebeskyttelseTilgangskontroll(slettNotatHendelse.utførtAv)
        tilstand.slettNotat(this, slettNotatHendelse)
    }

    fun returnerTilSaksbehandling(returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse) {
        tilstand.returnerTilSaksbehandling(this, returnerTilSaksbehandlingHendelse)
    }

    fun oppgaverPåVentMedUtgåttFrist(hendelse: PåVentFristUtgåttHendelse) {
        tilstand.oppgavePåVentMedUtgåttFrist(this, hendelse)
    }

    fun erEierAvOppgave(saksbehandler: Saksbehandler): Boolean = this.behandlerIdent == saksbehandler.navIdent

    private fun endreTilstand(
        nyTilstand: Tilstand,
        hendelse: Hendelse,
    ) {
        logger.info {
            "Endrer tilstand fra ${this.tilstand.type} til ${nyTilstand.type} for oppgaveId: ${this.oppgaveId} " +
                "basert på hendelse: ${hendelse.javaClass.simpleName} "
        }
        this.tilstand = nyTilstand
        this.tilstandslogg().leggTil(nyTilstand.type, hendelse)
    }

    fun sisteSaksbehandler(): String? =
        runCatching {
            tilstandslogg().firstOrNull { it.tilstand == UNDER_BEHANDLING && it.hendelse is AnsvarHendelse }?.let {
                (it.hendelse as AnsvarHendelse).ansvarligIdent
            }
        }.onFailure { e -> logger.error(e) { "Feil ved henting av siste saksbehandler for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()

    fun sisteBeslutter(): String? =
        runCatching {
            tilstandslogg().firstOrNull { it.tilstand == UNDER_KONTROLL && it.hendelse is AnsvarHendelse }?.let {
                (it.hendelse as AnsvarHendelse).ansvarligIdent
            }
        }.onFailure { e -> logger.error(e) { "Feil ved henting av siste beslutter for oppgave:  ${this.oppgaveId}" } }
            .getOrThrow()

    fun soknadId(): UUID? =
        runCatching {
            tilstandslogg().firstOrNull { it.hendelse is ForslagTilVedtakHendelse }?.let {
                val hendelse = it.hendelse as ForslagTilVedtakHendelse
                when (hendelse.behandletHendelseType) {
                    "Søknad" -> UUID.fromString(hendelse.behandletHendelseId)
                    "Manuell", "Meldekort" -> {
                        logger.info {
                            "behandletHendelseType is ${hendelse.behandletHendelseType} " +
                                "for oppgave: ${this.oppgaveId} " +
                                "søknadId eksisterer derfor ikke"
                        }
                        null
                    }

                    else -> {
                        logger.error { "Ukjent behandletHendelseType ${hendelse.behandletHendelseType} for oppgave ${this.oppgaveId}" }
                        null
                    }
                }
            }
        }.onFailure { e ->
            logger.error(e) { "Feil ved henting av ForslagTilVedtakHendelse og dermed søknadId for oppgave:  ${this.oppgaveId}" }
        }.getOrThrow()

    object Opprettet : Tilstand {
        override val type: Type = OPPRETTET

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpenger,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            oppgave.endreTilstand(KlarTilBehandling, forslagTilVedtakHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun avbryt(
            oppgave: RettTilDagpenger,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }
    }

    object KlarTilBehandling : Tilstand {
        override val type: Type = KLAR_TIL_BEHANDLING

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpenger,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand $type" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun tildel(
            oppgave: RettTilDagpenger,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun avbryt(
            oppgave: RettTilDagpenger,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }
    }

    object UnderBehandling : Tilstand {
        override val type: Type = UNDER_BEHANDLING

        override fun sendTilKontroll(
            oppgave: RettTilDagpenger,
            sendTilKontrollHendelse: SendTilKontrollHendelse,
        ) {
            requireEierskapTilOppgave(
                oppgave,
                sendTilKontrollHendelse.utførtAv,
                sendTilKontrollHendelse.javaClass.simpleName,
            )

            if (oppgave.meldingOmVedtak.kilde == GOSYS) {
                oppgave.meldingOmVedtak.kontrollertGosysBrev = NEI
            }
            if (oppgave.sisteBeslutter() == null) {
                oppgave.behandlerIdent = null
                oppgave.endreTilstand(KlarTilKontroll, sendTilKontrollHendelse)
            } else {
                oppgave.behandlerIdent = oppgave.sisteBeslutter()
                oppgave.endreTilstand(UnderKontroll(), sendTilKontrollHendelse)
                oppgave.emneknagger.add(TIDLIGERE_KONTROLLERT)
                oppgave.emneknagger.remove(RETUR_FRA_KONTROLL)
            }
        }

        override fun fjernAnsvar(
            oppgave: RettTilDagpenger,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
        }

        override fun avbryt(
            oppgave: RettTilDagpenger,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }

        override fun tildel(
            oppgave: RettTilDagpenger,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            if (oppgave.behandlerIdent != settOppgaveAnsvarHendelse.ansvarligIdent) {
                sikkerlogg.warn {
                    "Kan ikke tildele oppgave med id ${oppgave.oppgaveId} til ${settOppgaveAnsvarHendelse.ansvarligIdent}. " +
                        "Oppgave er allerede tildelt ${oppgave.behandlerIdent}."
                }
                throw AlleredeTildeltException(
                    "Kan ikke tildele oppgave til annen saksbehandler. Oppgaven er allerede tildelt.",
                )
            }
        }

        override fun utsett(
            oppgave: RettTilDagpenger,
            utsettOppgaveHendelse: UtsettOppgaveHendelse,
        ) {
            oppgave.emneknagger.add(utsettOppgaveHendelse.årsak.visningsnavn)
            oppgave.endreTilstand(PåVent, utsettOppgaveHendelse)
            oppgave.behandlerIdent =
                when (utsettOppgaveHendelse.beholdOppgave) {
                    true -> utsettOppgaveHendelse.navIdent
                    false -> null
                }
            oppgave.utsattTil = utsettOppgaveHendelse.utsattTil
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpenger,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun endreMeldingOmVedtakKilde(
            oppgave: RettTilDagpenger,
            endreMeldingOmVedtakKildeHendelse: EndreMeldingOmVedtakKildeHendelse,
        ) {
            logger.info {
                "Endrer kilde for melding om vedtak fra ${oppgave.meldingOmVedtak.kilde.name} til " +
                    endreMeldingOmVedtakKildeHendelse.meldingOmVedtakKilde.name
            }
            oppgave.meldingOmVedtak.kilde = endreMeldingOmVedtakKildeHendelse.meldingOmVedtakKilde
            oppgave.meldingOmVedtak.kontrollertGosysBrev = IKKE_RELEVANT
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            when (vedtakFattetHendelse.automatiskBehandlet) {
                true -> {
                    logger.info { "Mottok automatisk behandlet vedtak fattet i tilstand $type. Ferdigstiller oppgave." }
                    oppgave.behandlerIdent = null
                    oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
                    return Handling.LAGRE_OPPGAVE
                }

                else -> {
                    logger.info { "Mottok vedtak fattet i tilstand $type. Ignorerer meldingen." }
                    return Handling.INGEN
                }
            }
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ): FerdigstillBehandling {
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = godkjentBehandlingHendelse.utførtAv,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelse)
            return FerdigstillBehandling.GODKJENN
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            godkjentBehandlingHendelseUtenMeldingOmVedtak: GodkjentBehandlingHendelseUtenMeldingOmVedtak,
        ): FerdigstillBehandling {
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = godkjentBehandlingHendelseUtenMeldingOmVedtak.utførtAv,
                hendelseNavn = godkjentBehandlingHendelseUtenMeldingOmVedtak.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelseUtenMeldingOmVedtak)
            return FerdigstillBehandling.GODKJENN
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            avbruttHendelse: AvbruttHendelse,
        ) {
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = avbruttHendelse.utførtAv,
                hendelseNavn = avbruttHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, avbruttHendelse)
        }

        override fun avbryt(
            oppgave: RettTilDagpenger,
            avbruttHendelse: AvbruttHendelse,
        ) {
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = avbruttHendelse.utførtAv,
                hendelseNavn = avbruttHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(Avbrutt, avbruttHendelse)
        }

        override fun avbryt(
            oppgave: RettTilDagpenger,
            avbrytOppgaveHendelse: AvbrytOppgaveHendelse,
        ) {
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = avbrytOppgaveHendelse.utførtAv,
                hendelseNavn = avbrytOppgaveHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(Avbrutt, avbrytOppgaveHendelse)
        }
    }

    class AlleredeTildeltException(
        message: String,
    ) : RuntimeException(message)

    object FerdigBehandlet : Tilstand {
        override val type: Type = FERDIG_BEHANDLET

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            logger.warn { "Mottok vedtak fattet i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpenger,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.warn { "Mottok forslagTilVedtakHendelse i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }
    }

    object Avbrutt : Tilstand {
        override val type: Type = AVBRUTT

        override fun avbryt(
            oppgave: RettTilDagpenger,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            logger.info { "Behandling er allerede avbrutt." }
        }
    }

    object PåVent : Tilstand {
        override val type: Type = PAA_VENT

        override fun tildel(
            oppgave: RettTilDagpenger,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
            oppgave.utsattTil = null
        }

        override fun fjernAnsvar(
            oppgave: RettTilDagpenger,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
            oppgave.utsattTil = null
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpenger,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun avbryt(
            oppgave: RettTilDagpenger,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }

        override fun oppgavePåVentMedUtgåttFrist(
            oppgave: RettTilDagpenger,
            hendelse: PåVentFristUtgåttHendelse,
        ) {
            val nyTilstand =
                if (oppgave.behandlerIdent == null) {
                    KlarTilBehandling
                } else {
                    UnderBehandling
                }
            oppgave.endreTilstand(nyTilstand, hendelse)
            oppgave.utsattTil = null
            oppgave.emneknagger.add(Emneknagg.PåVent.TIDLIGERE_UTSATT.visningsnavn)
        }
    }

    object KlarTilKontroll : Tilstand {
        override val type: Type = KLAR_TIL_KONTROLL

        override fun tildel(
            oppgave: RettTilDagpenger,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            requireBeslutterTilgang(
                saksbehandler = settOppgaveAnsvarHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = settOppgaveAnsvarHendelse.utførtAv,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )

            oppgave.endreTilstand(UnderKontroll(), settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
        }

        override fun avbryt(
            oppgave: RettTilDagpenger,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }
    }

    object AvventerLåsAvBehandling : Tilstand {
        override val type: Type = AVVENTER_LÅS_AV_BEHANDLING
    }

    object AvventerOpplåsingAvBehandling : Tilstand {
        override val type: Type = AVVENTER_OPPLÅSING_AV_BEHANDLING
    }

    data class UnderKontroll(
        private var notat: Notat? = null,
    ) : Tilstand {
        override val type: Type = UNDER_KONTROLL

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            when (vedtakFattetHendelse.automatiskBehandlet) {
                true -> {
                    logger.info { "Mottok automatisk behandlet vedtak fattet i tilstand $type. Ferdigstiller oppgave." }
                    oppgave.behandlerIdent = null
                    oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
                    return Handling.LAGRE_OPPGAVE
                }

                else -> {
                    logger.info { "Mottok vedtak fattet i tilstand $type. Ignorerer meldingen." }
                    return Handling.INGEN
                }
            }
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ): FerdigstillBehandling {
            requireBeslutterTilgang(
                saksbehandler = godkjentBehandlingHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = godkjentBehandlingHendelse.utførtAv,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = godkjentBehandlingHendelse.utførtAv,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            requireKvittertGosysBrev(
                oppgave = oppgave,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelse)
            return BESLUTT
        }

        override fun ferdigstill(
            oppgave: RettTilDagpenger,
            godkjentBehandlingHendelseUtenMeldingOmVedtak: GodkjentBehandlingHendelseUtenMeldingOmVedtak,
        ): FerdigstillBehandling {
            requireBeslutterTilgang(
                saksbehandler = godkjentBehandlingHendelseUtenMeldingOmVedtak.utførtAv,
                tilstandType = type,
                hendelseNavn = godkjentBehandlingHendelseUtenMeldingOmVedtak.javaClass.simpleName,
            )
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = godkjentBehandlingHendelseUtenMeldingOmVedtak.utførtAv,
                hendelseNavn = godkjentBehandlingHendelseUtenMeldingOmVedtak.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = godkjentBehandlingHendelseUtenMeldingOmVedtak.utførtAv,
                hendelseNavn = godkjentBehandlingHendelseUtenMeldingOmVedtak.javaClass.simpleName,
            )

            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelseUtenMeldingOmVedtak)
            return BESLUTT
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpenger,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.warn { "Mottok forslagTilVedtakHendelse i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }

        override fun tildel(
            oppgave: RettTilDagpenger,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = settOppgaveAnsvarHendelse.utførtAv,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = settOppgaveAnsvarHendelse.utførtAv,
                hendelseNavn = settOppgaveAnsvarHendelse.javaClass.simpleName,
            )
        }

        override fun lagreBrevKvittering(
            oppgave: RettTilDagpenger,
            lagreBrevKvitteringHendelse: LagreBrevKvitteringHendelse,
        ) {
            requireBeslutterTilgang(
                saksbehandler = lagreBrevKvitteringHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = lagreBrevKvitteringHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = lagreBrevKvitteringHendelse.utførtAv,
                hendelseNavn = lagreBrevKvitteringHendelse.javaClass.simpleName,
            )
            if (oppgave.meldingOmVedtak.kilde == GOSYS) {
                require(lagreBrevKvitteringHendelse.kontrollertBrev != IKKE_RELEVANT)
            }
            oppgave.meldingOmVedtak.kontrollertGosysBrev = lagreBrevKvitteringHendelse.kontrollertBrev
        }

        override fun returnerTilSaksbehandling(
            oppgave: RettTilDagpenger,
            returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse,
        ) {
            requireBeslutterTilgang(
                saksbehandler = returnerTilSaksbehandlingHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )
            requireEierskapTilOppgave(
                oppgave = oppgave,
                saksbehandler = returnerTilSaksbehandlingHendelse.utførtAv,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )
            requireBeslutterUlikSaksbehandler(
                oppgave = oppgave,
                beslutter = returnerTilSaksbehandlingHendelse.utførtAv,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )

            oppgave.endreTilstand(UnderBehandling, returnerTilSaksbehandlingHendelse)
            oppgave.behandlerIdent = oppgave.sisteSaksbehandler()
            oppgave.emneknagger.add(RETUR_FRA_KONTROLL)
            oppgave.emneknagger.remove(TIDLIGERE_KONTROLLERT)
            if (oppgave.meldingOmVedtak.kilde == GOSYS) {
                oppgave.meldingOmVedtak.kontrollertGosysBrev = NEI
            }
        }

        override fun fjernAnsvar(
            oppgave: RettTilDagpenger,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilKontroll, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
        }

        override fun avbryt(
            oppgave: RettTilDagpenger,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }

        override fun lagreNotat(
            oppgave: RettTilDagpenger,
            notatHendelse: NotatHendelse,
        ) {
            when (notat) {
                null -> {
                    notat =
                        Notat(
                            notatId = UUIDv7.ny(),
                            tekst = notatHendelse.tekst,
                            sistEndretTidspunkt = LocalDateTime.now(),
                            skrevetAv = notatHendelse.utførtAv.navIdent,
                        )
                }

                else -> {
                    notat?.endreTekst(notatHendelse.tekst)
                }
            }
        }

        override fun slettNotat(
            oppgave: RettTilDagpenger,
            slettNotatHendelse: SlettNotatHendelse,
        ) {
            notat = null
        }

        override fun notat(): Notat? = notat
    }

    enum class FerdigstillBehandling {
        BESLUTT,
        GODKJENN,
    }

    enum class Handling {
        LAGRE_OPPGAVE,
        INGEN,
    }

    enum class KontrollertBrev {
        JA,
        NEI,
        IKKE_RELEVANT,
    }

    data class MeldingOmVedtak(
        var kilde: MeldingOmVedtakKilde,
        var kontrollertGosysBrev: KontrollertBrev,
    )

    enum class MeldingOmVedtakKilde {
        DP_SAK,
        GOSYS,
        INGEN,
    }

    sealed interface Tilstand {
        val type: Type

        enum class Type {
            OPPRETTET,
            KLAR_TIL_BEHANDLING,
            UNDER_BEHANDLING,
            FERDIG_BEHANDLET,
            PAA_VENT,
            KLAR_TIL_KONTROLL,
            UNDER_KONTROLL,
            AVVENTER_LÅS_AV_BEHANDLING,
            AVVENTER_OPPLÅSING_AV_BEHANDLING,
            AVBRUTT,
            ;

            companion object {
                val values
                    get() = entries.toSet()

                // Tilstander som ikke lenger er i bruk, skal ikke kunne søkes på
                val søkbareTilstander =
                    entries
                        .toSet()
                        .minus(OPPRETTET)
                        .minus(AVVENTER_LÅS_AV_BEHANDLING)
                        .minus(AVVENTER_OPPLÅSING_AV_BEHANDLING)
            }
        }

        fun notat(): Notat? = null

        fun behov() = emptySet<String>()

        fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpenger,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om forslag til vedtak i tilstand $type",
            )
        }

        fun avbryt(
            oppgave: RettTilDagpenger,
            avbruttHendelse: AvbruttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke avbryte oppgave i tilstand $type for " +
                        "${avbruttHendelse.javaClass.simpleName}",
            )
        }

        fun avbryt(
            oppgave: RettTilDagpenger,
            avbrytOppgaveHendelse: AvbrytOppgaveHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke avbryte oppgave i tilstand $type for " +
                        "${avbrytOppgaveHendelse.javaClass.simpleName}",
            )
        }

        fun ferdigstill(
            oppgave: RettTilDagpenger,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke ferdigstille oppgave i tilstand $type for " +
                        "${vedtakFattetHendelse.javaClass.simpleName}",
            )
        }

        fun ferdigstill(
            oppgave: RettTilDagpenger,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ): FerdigstillBehandling {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke ferdigstille oppgave i tilstand $type for " +
                        "${godkjentBehandlingHendelse.javaClass.simpleName}",
            )
        }

        fun ferdigstill(
            oppgave: RettTilDagpenger,
            godkjentBehandlingHendelseUtenMeldingOmVedtak: GodkjentBehandlingHendelseUtenMeldingOmVedtak,
        ): FerdigstillBehandling {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke ferdigstille oppgave i tilstand $type for " +
                        "${godkjentBehandlingHendelseUtenMeldingOmVedtak.javaClass.simpleName}",
            )
        }

        fun ferdigstill(
            oppgave: RettTilDagpenger,
            avbruttHendelse: AvbruttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message =
                    "Kan ikke ferdigstille oppgave i tilstand $type for " +
                        "${avbruttHendelse.javaClass.simpleName}",
            )
        }

        fun fjernAnsvar(
            oppgave: RettTilDagpenger,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om fjerne oppgaveansvar i tilstand $type",
            )
        }

        fun tildel(
            oppgave: RettTilDagpenger,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å tildele oppgave i tilstand $type",
            )
        }

        fun utsett(
            oppgave: RettTilDagpenger,
            utsettOppgaveHendelse: UtsettOppgaveHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å utsette oppgave i tilstand $type",
            )
        }

        fun sendTilKontroll(
            oppgave: RettTilDagpenger,
            sendTilKontrollHendelse: SendTilKontrollHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å sende til kontroll i tilstand $type",
            )
        }

        fun avbryt(
            oppgave: RettTilDagpenger,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om behandling avbrutt i tilstand $type",
            )
        }

        fun returnerTilSaksbehandling(
            oppgave: RettTilDagpenger,
            returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å returnere til saksbehandling fra kontroll i tilstand $type",
            )
        }

        fun lagreBrevKvittering(
            oppgave: RettTilDagpenger,
            lagreBrevKvitteringHendelse: LagreBrevKvitteringHendelse,
        ): Unit = throw UlovligKvitteringAvKontrollertBrev("Lagring av brevkvittering er ikke tillat i tilstand $type")

        fun endreMeldingOmVedtakKilde(
            oppgave: RettTilDagpenger,
            endreMeldingOmVedtakKildeHendelse: EndreMeldingOmVedtakKildeHendelse,
        ): Unit = throw UlovligEndringAvKildeForMeldingOmVedtak("Endring av kilde for melding om vedtak er ikke tillatt i tilstand $type")

        fun lagreNotat(
            oppgave: RettTilDagpenger,
            notatHendelse: NotatHendelse,
        ): Unit = throw RuntimeException("Notat er ikke tillatt i tilstand $type")

        fun slettNotat(
            oppgave: RettTilDagpenger,
            slettNotatHendelse: SlettNotatHendelse,
        ): Unit = throw RuntimeException("Kan ikke slette notat i tilstand $type")

        fun oppgavePåVentMedUtgåttFrist(
            oppgave: RettTilDagpenger,
            hendelse: PåVentFristUtgåttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å sette utgått frist for oppgave på vent i tilstand $type",
            )
        }

        private fun ulovligTilstandsendring(
            oppgaveId: UUID,
            message: String,
        ): Nothing {
            withLoggingContext("oppgaveId" to oppgaveId.toString()) {
                logger.error { message }
            }
            throw UlovligTilstandsendringException(message)
        }

        class ManglendeBeslutterTilgang(
            message: String,
        ) : ManglendeTilgang(message)

        class KanIkkeBeslutteEgenSaksbehandling(
            message: String,
        ) : ManglendeTilgang(message)

        class UlovligTilstandsendringException(
            message: String,
        ) : RuntimeException(message)

        class KreverKontrollAvGosysBrev(
            message: String,
        ) : RuntimeException(message)

        class UlovligEndringAvKildeForMeldingOmVedtak(
            message: String,
        ) : RuntimeException(message)

        class UlovligKvitteringAvKontrollertBrev(
            message: String,
        ) : RuntimeException(message)

        class UgyldigTilstandException(
            message: String,
        ) : RuntimeException(message)
    }
}
