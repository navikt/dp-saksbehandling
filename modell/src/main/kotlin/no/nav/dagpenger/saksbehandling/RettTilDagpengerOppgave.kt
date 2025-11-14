package no.nav.dagpenger.saksbehandling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.RettTilDagpengerOppgave.FerdigstillBehandling.BESLUTT
import no.nav.dagpenger.saksbehandling.RettTilDagpengerOppgave.RettTilDagpengerTilstand
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.EndreMeldingOmVedtakKildeHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelseUtenMeldingOmVedtak
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

data class RettTilDagpengerOppgave private constructor(
    override val oppgaveId: UUID,
    override val opprettet: LocalDateTime,
    override var behandlerIdent: String? = null,
    override val emneknagger: MutableSet<String>,
    override var utsattTil: LocalDate? = null,
    override val tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
    override val person: Person,
    override val behandling: RettTilDagpengerBehandling,
    override var meldingOmVedtak: MeldingOmVedtak,
    override var tilstand: RettTilDagpengerTilstand = KlarTilBehandling,
) : Oppgave<RettTilDagpengerTilstand>() {
    constructor(
        oppgaveId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: LocalDateTime,
        tilstandType: Tilstand.Type = Tilstand.Type.KLAR_TIL_BEHANDLING,
        behandlerIdent: String? = null,
        tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
        person: Person,
        behandling: RettTilDagpengerBehandling,
        meldingOmVedtak: MeldingOmVedtak,
    ) : this(
        oppgaveId = oppgaveId,
        behandlerIdent = behandlerIdent,
        opprettet = opprettet,
        emneknagger = emneknagger.toMutableSet(),
        tilstandslogg = tilstandslogg,
        person = person,
        behandling = behandling,
        meldingOmVedtak = meldingOmVedtak,
    ) {
        this.tilstand =
            when (tilstandType) {
                Tilstand.Type.KLAR_TIL_BEHANDLING -> KlarTilBehandling
                Tilstand.Type.UNDER_BEHANDLING -> UnderBehandling
                Tilstand.Type.PAA_VENT -> PåVent
                Tilstand.Type.KLAR_TIL_KONTROLL -> KlarTilKontroll
                Tilstand.Type.UNDER_KONTROLL -> UnderKontroll(null)
                Tilstand.Type.FERDIG_BEHANDLET -> FerdigBehandlet
                Tilstand.Type.AVBRUTT -> Avbrutt
                Tilstand.Type.OPPRETTET -> Opprettet
                Tilstand.Type.AVVENTER_LÅS_AV_BEHANDLING -> AvventerLåsAvBehandling
                Tilstand.Type.AVVENTER_OPPLÅSING_AV_BEHANDLING -> AvventerOpplåsingAvBehandling
            }
    }

    constructor(
        oppgaveId: UUID,
        emneknagger: Set<String> = emptySet(),
        opprettet: LocalDateTime,
        behandlerIdent: String? = null,
        tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
        person: Person,
        behandling: RettTilDagpengerBehandling,
        meldingOmVedtak: MeldingOmVedtak,
        tilstand: RettTilDagpengerTilstand,
    ) : this(
        oppgaveId = oppgaveId,
        behandlerIdent = behandlerIdent,
        opprettet = opprettet,
        emneknagger = emneknagger.toMutableSet(),
        tilstandType = tilstand.type,
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
            tilstand: RettTilDagpengerTilstand,
            utsattTil: LocalDate?,
            tilstandslogg: OppgaveTilstandslogg = OppgaveTilstandslogg(),
            person: Person,
            behandling: RettTilDagpengerBehandling,
            meldingOmVedtak: MeldingOmVedtak,
        ): RettTilDagpengerOppgave =
            RettTilDagpengerOppgave(
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

        private fun requireBeslutterTilgang(
            saksbehandler: Saksbehandler,
            tilstandType: Tilstand.Type,
            hendelseNavn: String,
        ) {
            require(saksbehandler.tilganger.contains(BESLUTTER)) {
                throw RettTilDagpengerTilstand.ManglendeBeslutterTilgang(
                    "Kan ikke behandle $hendelseNavn i tilstand $tilstandType uten beslutter-tilgang",
                )
            }
        }

        private fun requireBeslutterUlikSaksbehandler(
            oppgave: Oppgave<*>,
            beslutter: Saksbehandler,
            hendelseNavn: String,
        ) {
            require(oppgave.sisteSaksbehandler() != beslutter.navIdent) {
                throw RettTilDagpengerTilstand.KanIkkeBeslutteEgenSaksbehandling(
                    "Ulovlig hendelse $hendelseNavn på oppgave i tilstand ${oppgave.tilstandType()}. " +
                        "Oppgave kan ikke behandles og kontrolleres av samme person. Saksbehandler på oppgaven er " +
                        "${oppgave.sisteSaksbehandler()} og kan derfor ikke kontrolleres av ${beslutter.navIdent}",
                )
            }
        }

        private fun requireKvittertGosysBrev(
            oppgave: RettTilDagpengerOppgave,
            hendelseNavn: String,
        ) {
            if (oppgave.meldingOmVedtak.kilde == GOSYS) {
                require(oppgave.meldingOmVedtak.kontrollertGosysBrev == KontrollertBrev.JA) {
                    throw RettTilDagpengerTilstand.KreverKontrollAvGosysBrev(
                        "Brev i Gosys må være kontrollert av beslutter for å kunne behandle $hendelseNavn i " +
                            "tilstand ${oppgave.tilstand.type}. Brevkilde: ${oppgave.meldingOmVedtak.kilde}, " +
                            "KontrollertGosysBrev: ${oppgave.meldingOmVedtak.kontrollertGosysBrev}",
                    )
                }
            }
        }
    }

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

//    override fun endreTilstand(
//        nyTilstand: Tilstand,
//        hendelse: Hendelse,
//    ) {
//        logger.info {
//            "Endrer tilstand fra ${this.tilstand.type} til ${nyTilstand.type} for oppgaveId: ${this.oppgaveId} " +
//                    "basert på hendelse: ${hendelse.javaClass.simpleName} "
//        }
//        this.tilstand = nyTilstand as RettTilDagpengerTilstand
//        this.tilstandslogg().leggTil(nyTilstand.type, hendelse)
//    }
//    override fun endreTilstand(
//        nyTilstand: Tilstand,
//        hendelse: Hendelse,
//    ) {
//        logger.info {
//            "Endrer tilstand fra ${this.tilstand.type} til ${nyTilstand.type} for oppgaveId: ${this.oppgaveId} " +
//                    "basert på hendelse: ${hendelse.javaClass.simpleName} "
//        }
//        this.tilstand = nyTilstand as RettTilDagpengerTilstand
//        this.tilstandslogg().leggTil(nyTilstand.type, hendelse)
//    }

    object Opprettet : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = OPPRETTET

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpengerOppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            oppgave.endreTilstand(KlarTilBehandling, forslagTilVedtakHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }

        override fun ferdigstill(
            oppgave: RettTilDagpengerOppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }
    }

    object KlarTilBehandling : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = Tilstand.Type.KLAR_TIL_BEHANDLING

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpengerOppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand $type" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun tildel(
            oppgave: Oppgave<*>,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
        }

        override fun ferdigstill(
            oppgave: RettTilDagpengerOppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }
    }

    object UnderBehandling : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = UNDER_BEHANDLING

        override fun sendTilKontroll(
            oppgave: RettTilDagpengerOppgave,
            sendTilKontrollHendelse: SendTilKontrollHendelse,
        ) {
            oppgave.requireEierskapTilOppgave(
                sendTilKontrollHendelse.utførtAv,
                sendTilKontrollHendelse.javaClass.simpleName,
            )

            if (oppgave.meldingOmVedtak.kilde == GOSYS) {
                oppgave.meldingOmVedtak.kontrollertGosysBrev = KontrollertBrev.NEI
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
            oppgave: RettTilDagpengerOppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
        }

        override fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }

        override fun tildel(
            oppgave: Oppgave<*>,
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
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun endreMeldingOmVedtakKilde(
            oppgave: RettTilDagpengerOppgave,
            endreMeldingOmVedtakKildeHendelse: EndreMeldingOmVedtakKildeHendelse,
        ) {
            logger.info {
                "Endrer kilde for melding om vedtak fra ${oppgave.meldingOmVedtak.kilde.name} til " +
                    endreMeldingOmVedtakKildeHendelse.meldingOmVedtakKilde.name
            }
            oppgave.meldingOmVedtak.kilde = endreMeldingOmVedtakKildeHendelse.meldingOmVedtakKilde
            oppgave.meldingOmVedtak.kontrollertGosysBrev = KontrollertBrev.IKKE_RELEVANT
        }

        override fun ferdigstill(
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ): FerdigstillBehandling {
            oppgave.requireEierskapTilOppgave(
                saksbehandler = godkjentBehandlingHendelse.utførtAv,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelse)
            return FerdigstillBehandling.GODKJENN
        }

        override fun ferdigstill(
            oppgave: RettTilDagpengerOppgave,
            godkjentBehandlingHendelseUtenMeldingOmVedtak: GodkjentBehandlingHendelseUtenMeldingOmVedtak,
        ): FerdigstillBehandling {
            oppgave.requireEierskapTilOppgave(
                saksbehandler = godkjentBehandlingHendelseUtenMeldingOmVedtak.utførtAv,
                hendelseNavn = godkjentBehandlingHendelseUtenMeldingOmVedtak.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, godkjentBehandlingHendelseUtenMeldingOmVedtak)
            return FerdigstillBehandling.GODKJENN
        }

        override fun ferdigstill(
            oppgave: RettTilDagpengerOppgave,
            avbruttHendelse: AvbruttHendelse,
        ) {
            oppgave.requireEierskapTilOppgave(
                saksbehandler = avbruttHendelse.utførtAv,
                hendelseNavn = avbruttHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(FerdigBehandlet, avbruttHendelse)
        }

        override fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            avbruttHendelse: AvbruttHendelse,
        ) {
            oppgave.requireEierskapTilOppgave(
                saksbehandler = avbruttHendelse.utførtAv,
                hendelseNavn = avbruttHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(Avbrutt, avbruttHendelse)
        }

        override fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            avbrytOppgaveHendelse: AvbrytOppgaveHendelse,
        ) {
            oppgave.requireEierskapTilOppgave(
                saksbehandler = avbrytOppgaveHendelse.utførtAv,
                hendelseNavn = avbrytOppgaveHendelse.javaClass.simpleName,
            )
            oppgave.endreTilstand(Avbrutt, avbrytOppgaveHendelse)
        }
    }

    class AlleredeTildeltException(
        message: String,
    ) : RuntimeException(message)

    object FerdigBehandlet : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = FERDIG_BEHANDLET

        override fun ferdigstill(
            oppgave: RettTilDagpengerOppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            logger.warn { "Mottok vedtak fattet i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpengerOppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.warn { "Mottok forslagTilVedtakHendelse i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }
    }

    object Avbrutt : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = Tilstand.Type.AVBRUTT

        override fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            logger.info { "Behandling er allerede avbrutt." }
        }
    }

    object PåVent : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = Tilstand.Type.PAA_VENT

        override fun tildel(
            oppgave: Oppgave<*>,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(UnderBehandling, settOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = settOppgaveAnsvarHendelse.ansvarligIdent
            oppgave.utsattTil = null
        }

        override fun fjernAnsvar(
            oppgave: RettTilDagpengerOppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilBehandling, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
            oppgave.utsattTil = null
        }

        override fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpengerOppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.info { "Nytt forslag til vedtak mottatt for oppgaveId: ${oppgave.oppgaveId} i tilstand ${type.name}" }
            return Handling.LAGRE_OPPGAVE
        }

        override fun ferdigstill(
            oppgave: RettTilDagpengerOppgave,
            vedtakFattetHendelse: VedtakFattetHendelse,
        ): Handling {
            oppgave.endreTilstand(FerdigBehandlet, vedtakFattetHendelse)
            return Handling.LAGRE_OPPGAVE
        }

        override fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }

        override fun oppgavePåVentMedUtgåttFrist(
            oppgave: RettTilDagpengerOppgave,
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

    object KlarTilKontroll : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = KLAR_TIL_KONTROLL

        override fun tildel(
            oppgave: Oppgave<*>,
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
            oppgave: RettTilDagpengerOppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }
    }

    object AvventerLåsAvBehandling : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = AVVENTER_LÅS_AV_BEHANDLING
    }

    object AvventerOpplåsingAvBehandling : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = AVVENTER_OPPLÅSING_AV_BEHANDLING
    }

    data class UnderKontroll(
        private var notat: Notat? = null,
    ) : RettTilDagpengerTilstand {
        override val type: Tilstand.Type = UNDER_KONTROLL

        override fun ferdigstill(
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
            godkjentBehandlingHendelse: GodkjentBehandlingHendelse,
        ): FerdigstillBehandling {
            requireBeslutterTilgang(
                saksbehandler = godkjentBehandlingHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = godkjentBehandlingHendelse.javaClass.simpleName,
            )
            oppgave.requireEierskapTilOppgave(
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
            oppgave: RettTilDagpengerOppgave,
            godkjentBehandlingHendelseUtenMeldingOmVedtak: GodkjentBehandlingHendelseUtenMeldingOmVedtak,
        ): FerdigstillBehandling {
            requireBeslutterTilgang(
                saksbehandler = godkjentBehandlingHendelseUtenMeldingOmVedtak.utførtAv,
                tilstandType = type,
                hendelseNavn = godkjentBehandlingHendelseUtenMeldingOmVedtak.javaClass.simpleName,
            )
            oppgave.requireEierskapTilOppgave(
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
            oppgave: RettTilDagpengerOppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            logger.warn { "Mottok forslagTilVedtakHendelse i tilstand $type. Ignorerer meldingen." }
            return Handling.INGEN
        }

        override fun tildel(
            oppgave: Oppgave<*>,
            settOppgaveAnsvarHendelse: SettOppgaveAnsvarHendelse,
        ) {
            oppgave.requireEierskapTilOppgave(
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
            oppgave: RettTilDagpengerOppgave,
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
                require(lagreBrevKvitteringHendelse.kontrollertBrev != KontrollertBrev.IKKE_RELEVANT)
            }
            oppgave.meldingOmVedtak.kontrollertGosysBrev = lagreBrevKvitteringHendelse.kontrollertBrev
        }

        override fun returnerTilSaksbehandling(
            oppgave: RettTilDagpengerOppgave,
            returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse,
        ) {
            requireBeslutterTilgang(
                saksbehandler = returnerTilSaksbehandlingHendelse.utførtAv,
                tilstandType = type,
                hendelseNavn = returnerTilSaksbehandlingHendelse.javaClass.simpleName,
            )
            oppgave.requireEierskapTilOppgave(
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
                oppgave.meldingOmVedtak.kontrollertGosysBrev = KontrollertBrev.NEI
            }
        }

        override fun fjernAnsvar(
            oppgave: RettTilDagpengerOppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            oppgave.endreTilstand(KlarTilKontroll, fjernOppgaveAnsvarHendelse)
            oppgave.behandlerIdent = null
        }

        override fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            oppgave.endreTilstand(Avbrutt, behandlingAvbruttHendelse)
        }

        override fun lagreNotat(
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
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

    sealed interface RettTilDagpengerTilstand : Oppgave.Tilstand {
        fun behov() = emptySet<String>()

        fun oppgaveKlarTilBehandling(
            oppgave: RettTilDagpengerOppgave,
            forslagTilVedtakHendelse: ForslagTilVedtakHendelse,
        ): Handling {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om forslag til vedtak i tilstand $type",
            )
        }

        fun avbryt(
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
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
            oppgave: RettTilDagpengerOppgave,
            fjernOppgaveAnsvarHendelse: FjernOppgaveAnsvarHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om fjerne oppgaveansvar i tilstand $type",
            )
        }

        fun utsett(
            oppgave: RettTilDagpengerOppgave,
            utsettOppgaveHendelse: UtsettOppgaveHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å utsette oppgave i tilstand $type",
            )
        }

        fun sendTilKontroll(
            oppgave: RettTilDagpengerOppgave,
            sendTilKontrollHendelse: SendTilKontrollHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å sende til kontroll i tilstand $type",
            )
        }

        fun avbryt(
            oppgave: RettTilDagpengerOppgave,
            behandlingAvbruttHendelse: BehandlingAvbruttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om behandling avbrutt i tilstand $type",
            )
        }

        fun returnerTilSaksbehandling(
            oppgave: RettTilDagpengerOppgave,
            returnerTilSaksbehandlingHendelse: ReturnerTilSaksbehandlingHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å returnere til saksbehandling fra kontroll i tilstand $type",
            )
        }

        fun lagreBrevKvittering(
            oppgave: RettTilDagpengerOppgave,
            lagreBrevKvitteringHendelse: LagreBrevKvitteringHendelse,
        ): Unit = throw UlovligKvitteringAvKontrollertBrev("Lagring av brevkvittering er ikke tillat i tilstand $type")

        fun endreMeldingOmVedtakKilde(
            oppgave: RettTilDagpengerOppgave,
            endreMeldingOmVedtakKildeHendelse: EndreMeldingOmVedtakKildeHendelse,
        ): Unit = throw UlovligEndringAvKildeForMeldingOmVedtak("Endring av kilde for melding om vedtak er ikke tillatt i tilstand $type")

        fun lagreNotat(
            oppgave: RettTilDagpengerOppgave,
            notatHendelse: NotatHendelse,
        ): Unit = throw RuntimeException("Notat er ikke tillatt i tilstand $type")

        fun slettNotat(
            oppgave: RettTilDagpengerOppgave,
            slettNotatHendelse: SlettNotatHendelse,
        ): Unit = throw RuntimeException("Kan ikke slette notat i tilstand $type")

        fun oppgavePåVentMedUtgåttFrist(
            oppgave: RettTilDagpengerOppgave,
            hendelse: PåVentFristUtgåttHendelse,
        ) {
            ulovligTilstandsendring(
                oppgaveId = oppgave.oppgaveId,
                message = "Kan ikke håndtere hendelse om å sette utgått frist for oppgave på vent i tilstand $type",
            )
        }

        class ManglendeBeslutterTilgang(
            message: String,
        ) : ManglendeTilgang(message)

        class KanIkkeBeslutteEgenSaksbehandling(
            message: String,
        ) : ManglendeTilgang(message)

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
