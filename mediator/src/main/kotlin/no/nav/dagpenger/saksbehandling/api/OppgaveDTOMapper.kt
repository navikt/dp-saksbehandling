package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.EmneknaggKategori
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.IKKE_RELEVANT
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.JA
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.NEI
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.INGEN
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.SikkerhetstiltakIntern
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.models.AdressebeskyttelseGraderingDTO
import no.nav.dagpenger.saksbehandling.api.models.AvbrytOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.api.models.BehandlingTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.EmneknaggDTO
import no.nav.dagpenger.saksbehandling.api.models.EmneknaggKategoriDTO
import no.nav.dagpenger.saksbehandling.api.models.KjonnDTO
import no.nav.dagpenger.saksbehandling.api.models.KontrollertBrevDTO
import no.nav.dagpenger.saksbehandling.api.models.LeggTilbakeAarsakDTO
import no.nav.dagpenger.saksbehandling.api.models.LovligeEndringerDTO
import no.nav.dagpenger.saksbehandling.api.models.MeldingOmVedtakKildeDTO
import no.nav.dagpenger.saksbehandling.api.models.NotatDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveHistorikkDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveOversiktResultatDTO
import no.nav.dagpenger.saksbehandling.api.models.OppgaveTilstandDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonDTO
import no.nav.dagpenger.saksbehandling.api.models.PersonOversiktDTO
import no.nav.dagpenger.saksbehandling.api.models.SakDTO
import no.nav.dagpenger.saksbehandling.api.models.SikkerhetstiltakDTO
import no.nav.dagpenger.saksbehandling.api.models.TildeltOppgaveDTO
import no.nav.dagpenger.saksbehandling.api.models.UtlostAvTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.UtsettOppgaveAarsakDTO
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.hentEmneknaggKategori
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.time.LocalDate
import java.util.UUID

internal class OppgaveDTOMapper(
    private val oppslag: Oppslag,
    private val oppgaveHistorikkDTOMapper: OppgaveHistorikkDTOMapper,
    private val sakMediator: SakMediator,
) {
    private fun SakHistorikk?.saker(oppgaver: List<OppgaveOversiktDTO>): List<SakDTO> =
        when (this) {
            null -> emptyList()
            else ->
                this.saker().map { sak ->
                    val behandlingIder = sak.behandlinger().map { it.behandlingId }.toSet()
                    SakDTO(
                        id = sak.sakId,
                        oppgaver = oppgaver.filter { it.behandlingId in behandlingIder },
                    )
                }
        }

    suspend fun lagPersonDTO(person: Person): PersonDTO {
        val pdlPerson = oppslag.hentPerson(person.ident)
        return lagPersonDTO(person, pdlPerson)
    }

    suspend fun lagPersonOversiktDTO(
        person: Person,
        oppgaver: List<OppgaveOversiktDTO>,
    ): PersonOversiktDTO {
        val sakHistorikk = sakMediator.finnSakHistorikk(ident = person.ident)
        return PersonOversiktDTO(
            person = lagPersonDTO(person = person),
            saker = sakHistorikk.saker(oppgaver),
            oppgaver = oppgaver,
        )
    }

    suspend fun lagOppgaveDTO(oppgave: Oppgave): OppgaveDTO =
        coroutineScope {
            val person = async { oppslag.hentPerson(oppgave.personIdent()) }
            val journalpostIder = async { oppslag.hentJournalpostIder(oppgave) }
            val sisteSaksbehandlerDTO =
                oppgave.sisteSaksbehandler()?.let { saksbehandlerIdent ->
                    async { oppslag.hentBehandler(saksbehandlerIdent) }
                }
            val sisteBeslutterDTO =
                oppgave.sisteBeslutter()?.let { beslutterIdent ->
                    async { oppslag.hentBehandler(beslutterIdent) }
                }

            val oppgaveHistorikk =
                async {
                    oppgaveHistorikkDTOMapper.lagOppgaveHistorikk(oppgave.tilstandslogg)
                }
            lagOppgaveDTO(
                oppgave = oppgave,
                person = person.await(),
                journalpostIder = journalpostIder.await(),
                sisteSaksbehandlerDTO = sisteSaksbehandlerDTO?.await(),
                sisteBeslutterDTO = sisteBeslutterDTO?.await(),
                oppgaveHistorikk = oppgaveHistorikk.await(),
                soknadId = oppgave.søknadId(),
            )
        }

    private fun lagOppgaveDTO(
        oppgave: Oppgave,
        person: PDLPersonIntern,
        journalpostIder: Set<String>,
        sisteSaksbehandlerDTO: BehandlerDTO? = null,
        sisteBeslutterDTO: BehandlerDTO? = null,
        oppgaveHistorikk: List<OppgaveHistorikkDTO> = emptyList(),
        soknadId: UUID? = null,
    ): OppgaveDTO =

        OppgaveDTO(
            oppgaveId = oppgave.oppgaveId,
            behandlingId = oppgave.behandling.behandlingId,
            person = lagPersonDTO(oppgave.person, person),
            tidspunktOpprettet = oppgave.opprettet,
            behandlingType = oppgave.tilBehandlingTypeDTO(),
            utlostAv = oppgave.tilUtlostAvTypeDTO(),
            emneknagger = oppgave.emneknagger.tilOppgaveEmneknaggerDTOListe(),
            tilstand = oppgave.tilstand().tilOppgaveTilstandDTO(),
            journalpostIder = journalpostIder.toList(),
            utsattTilDato = oppgave.utsattTil(),
            saksbehandler = sisteSaksbehandlerDTO,
            historikk = oppgaveHistorikk,
            beslutter = sisteBeslutterDTO,
            notat =
                oppgave.tilstand().notat()?.let {
                    NotatDTO(
                        tekst = it.hentTekst(),
                        sistEndretTidspunkt = it.sistEndretTidspunkt,
                    )
                },
            lovligeEndringer =
                LovligeEndringerDTO(
                    paaVentAarsaker = oppgave.lovligePåVentÅrsaker(),
                    avbrytAarsaker = oppgave.lovligeAvbrytÅrsaker(),
                    leggTilbakeAarsaker = oppgave.lovligeLeggTilbakeÅrsaker(),
                ),
            soknadId = soknadId,
            meldingOmVedtakKilde =
                when (oppgave.meldingOmVedtakKilde()) {
                    DP_SAK -> MeldingOmVedtakKildeDTO.DP_SAK
                    GOSYS -> MeldingOmVedtakKildeDTO.GOSYS
                    INGEN -> MeldingOmVedtakKildeDTO.INGEN
                },
            kontrollertBrev =
                when (oppgave.kontrollertBrev()) {
                    JA -> KontrollertBrevDTO.JA
                    NEI -> KontrollertBrevDTO.NEI
                    IKKE_RELEVANT -> KontrollertBrevDTO.IKKE_RELEVANT
                },
        )

    private fun mapGyldigeSikkerhetstiltak(person: PDLPersonIntern): List<SikkerhetstiltakDTO> =
        person.sikkerhetstiltak
            .map { sikkerhetstiltakDto ->
                SikkerhetstiltakIntern(
                    type = sikkerhetstiltakDto.type,
                    beskrivelse = sikkerhetstiltakDto.beskrivelse,
                    gyldigFom = sikkerhetstiltakDto.gyldigFom,
                    gyldigTom = sikkerhetstiltakDto.gyldigTom,
                )
            }.filter { sikkerhetstiltakIntern -> sikkerhetstiltakIntern.erGyldig(LocalDate.now()) }
            .map {
                SikkerhetstiltakDTO(beskrivelse = it.beskrivelse, gyldigTom = it.gyldigTom)
            }

    private fun lagPersonDTO(
        person: Person,
        pdlPersonIntern: PDLPersonIntern,
    ): PersonDTO =
        PersonDTO(
            ident = person.ident,
            id = person.id,
            fornavn = pdlPersonIntern.fornavn,
            etternavn = pdlPersonIntern.etternavn,
            mellomnavn = pdlPersonIntern.mellomnavn,
            fodselsdato = pdlPersonIntern.fødselsdato,
            alder = pdlPersonIntern.alder,
            kjonn =
                when (pdlPersonIntern.kjønn) {
                    PDLPerson.Kjonn.MANN -> KjonnDTO.MANN
                    PDLPerson.Kjonn.KVINNE -> KjonnDTO.KVINNE
                    PDLPerson.Kjonn.UKJENT -> KjonnDTO.UKJENT
                },
            statsborgerskap = pdlPersonIntern.statsborgerskap,
            skjermesSomEgneAnsatte = person.skjermesSomEgneAnsatte,
            sikkerhetstiltak = mapGyldigeSikkerhetstiltak(pdlPersonIntern),
            adressebeskyttelseGradering =
                when (person.adressebeskyttelseGradering) {
                    AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG_UTLAND
                    AdressebeskyttelseGradering.STRENGT_FORTROLIG -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG
                    AdressebeskyttelseGradering.FORTROLIG -> AdressebeskyttelseGraderingDTO.FORTROLIG
                    AdressebeskyttelseGradering.UGRADERT -> AdressebeskyttelseGraderingDTO.UGRADERT
                },
        )
}

fun Set<String>.tilOppgaveEmneknaggerDTOListe(): List<EmneknaggDTO> =
    this.map { visningsNavn ->
        val kategori = hentEmneknaggKategori(visningsNavn)
        EmneknaggDTO(
            visningsnavn = visningsNavn,
            kategori = kategori.tilDTO(),
        )
    }

private fun EmneknaggKategori.tilDTO(): EmneknaggKategoriDTO =
    when (this) {
        EmneknaggKategori.RETTIGHET -> EmneknaggKategoriDTO.RETTIGHET
        EmneknaggKategori.GJENOPPTAK -> EmneknaggKategoriDTO.GJENOPPTAK
        EmneknaggKategori.SØKNADSRESULTAT -> EmneknaggKategoriDTO.SOKNADSRESULTAT
        EmneknaggKategori.AVSLAGSGRUNN -> EmneknaggKategoriDTO.AVSLAGSGRUNN
        EmneknaggKategori.AVBRUTT_GRUNN -> EmneknaggKategoriDTO.AVBRUTT_GRUNN
        EmneknaggKategori.PÅ_VENT -> EmneknaggKategoriDTO.PAA_VENT
        EmneknaggKategori.ETTERSENDING -> EmneknaggKategoriDTO.ETTERSENDING
        EmneknaggKategori.UDEFINERT -> EmneknaggKategoriDTO.UDEFINERT
    }

internal fun Oppgave.tilOppgaveOversiktDTO() =
    OppgaveOversiktDTO(
        oppgaveId = this.oppgaveId,
        behandlingId = this.behandling.behandlingId,
        personIdent = this.personIdent(),
        tidspunktOpprettet = this.opprettet,
        behandlingType = this.tilBehandlingTypeDTO(),
        utlostAv = this.tilUtlostAvTypeDTO(),
        emneknagger = this.emneknagger.tilOppgaveEmneknaggerDTOListe(),
        skjermesSomEgneAnsatte = this.person.skjermesSomEgneAnsatte,
        adressebeskyttelseGradering =
            when (this.person.adressebeskyttelseGradering) {
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG_UTLAND
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> AdressebeskyttelseGraderingDTO.STRENGT_FORTROLIG
                AdressebeskyttelseGradering.FORTROLIG -> AdressebeskyttelseGraderingDTO.FORTROLIG
                AdressebeskyttelseGradering.UGRADERT -> AdressebeskyttelseGraderingDTO.UGRADERT
            },
        tilstand = this.tilstand().tilOppgaveTilstandDTO(),
        lovligeEndringer =
            LovligeEndringerDTO(
                paaVentAarsaker = this.lovligePåVentÅrsaker(),
                avbrytAarsaker = this.lovligeAvbrytÅrsaker(),
                leggTilbakeAarsaker = this.lovligeLeggTilbakeÅrsaker(),
            ),
        behandlerIdent = this.behandlerIdent,
        utsattTilDato = this.utsattTil(),
    )

internal fun List<Oppgave>.tilOppgaveOversiktDTOListe(): List<OppgaveOversiktDTO> = this.map { oppgave -> oppgave.tilOppgaveOversiktDTO() }

internal fun PostgresOppgaveRepository.OppgaveSøkResultat.tilOppgaverOversiktResultatDTO(): OppgaveOversiktResultatDTO =
    OppgaveOversiktResultatDTO(
        oppgaver = this.oppgaver.tilOppgaveOversiktDTOListe(),
        totaltAntallOppgaver = this.totaltAntallOppgaver,
    )

internal fun Oppgave.Tilstand.tilOppgaveTilstandDTO(): OppgaveTilstandDTO =
    when (this) {
        is Oppgave.Opprettet -> throw InternDataException("Ikke tillatt å eksponere oppgavetilstand Opprettet")
        is Oppgave.KlarTilBehandling -> OppgaveTilstandDTO.KLAR_TIL_BEHANDLING
        is Oppgave.UnderBehandling -> OppgaveTilstandDTO.UNDER_BEHANDLING
        is Oppgave.FerdigBehandlet -> OppgaveTilstandDTO.FERDIG_BEHANDLET
        is Oppgave.PåVent -> OppgaveTilstandDTO.PAA_VENT
        is Oppgave.KlarTilKontroll -> OppgaveTilstandDTO.KLAR_TIL_KONTROLL
        is Oppgave.UnderKontroll -> OppgaveTilstandDTO.UNDER_KONTROLL
        is Oppgave.AvventerLåsAvBehandling -> OppgaveTilstandDTO.AVVENTER_LÅS_AV_BEHANDLING
        is Oppgave.AvventerOpplåsingAvBehandling -> OppgaveTilstandDTO.AVVENTER_OPPLÅSING_AV_BEHANDLING
        is Oppgave.Avbrutt -> OppgaveTilstandDTO.AVBRUTT
        is Oppgave.AvbruttMaskinelt -> OppgaveTilstandDTO.AVBRUTT_MASKINELT
    }

internal fun Oppgave.tilTildeltOppgaveDTO(): TildeltOppgaveDTO =
    TildeltOppgaveDTO(
        nyTilstand = this.tilstand().tilOppgaveTilstandDTO(),
        behandlingType = this.tilBehandlingTypeDTO(),
        utlostAv = this.tilUtlostAvTypeDTO(),
    )

internal fun Oppgave.tilBehandlingTypeDTO(): BehandlingTypeDTO =
    when (this.behandling.utløstAv) {
        UtløstAvType.SØKNAD -> BehandlingTypeDTO.RETT_TIL_DAGPENGER
        UtløstAvType.MELDEKORT -> BehandlingTypeDTO.RETT_TIL_DAGPENGER
        UtløstAvType.MANUELL -> BehandlingTypeDTO.RETT_TIL_DAGPENGER
        UtløstAvType.KLAGE -> BehandlingTypeDTO.KLAGE
        UtløstAvType.INNSENDING -> BehandlingTypeDTO.INNSENDING
        UtløstAvType.OMGJØRING -> BehandlingTypeDTO.RETT_TIL_DAGPENGER
    }

internal fun Oppgave.tilUtlostAvTypeDTO(): UtlostAvTypeDTO =
    when (this.behandling.utløstAv) {
        UtløstAvType.SØKNAD -> UtlostAvTypeDTO.SØKNAD
        UtløstAvType.KLAGE -> UtlostAvTypeDTO.KLAGE
        UtløstAvType.MELDEKORT -> UtlostAvTypeDTO.MELDEKORT
        UtløstAvType.MANUELL -> UtlostAvTypeDTO.MANUELL
        UtløstAvType.INNSENDING -> UtlostAvTypeDTO.INNSENDING
        UtløstAvType.OMGJØRING -> UtlostAvTypeDTO.OMGJØRING
    }

internal fun Oppgave.lovligePåVentÅrsaker(): List<UtsettOppgaveAarsakDTO> =
    when (this.tilstand().type) {
        UNDER_BEHANDLING -> UtsettOppgaveAarsakDTO.entries
        else -> emptyList()
    }

internal fun Oppgave.lovligeAvbrytÅrsaker(): List<AvbrytOppgaveAarsakDTO> =
    when (this.tilstand().type) {
        UNDER_BEHANDLING -> AvbrytOppgaveAarsakDTO.entries
        else -> emptyList()
    }

internal fun Oppgave.lovligeLeggTilbakeÅrsaker(): List<LeggTilbakeAarsakDTO> =
    when (this.tilstand().type) {
        in setOf(UNDER_BEHANDLING, UNDER_KONTROLL) -> LeggTilbakeAarsakDTO.entries
        else -> emptyList()
    }
