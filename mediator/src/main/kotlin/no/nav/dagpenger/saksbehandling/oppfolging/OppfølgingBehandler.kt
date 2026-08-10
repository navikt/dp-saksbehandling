package no.nav.dagpenger.saksbehandling.oppfolging

import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.behandling.OpprettBehandlingTypeDTO
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppfølgingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageinstansVedtak
import java.util.UUID

class OppfølgingBehandler(
    private val klageMediator: KlageMediator,
    private val behandlingKlient: BehandlingKlient,
) {
    fun opprettKlage(
        oppfølging: Oppfølging,
        hendelse: FerdigstillOppfølgingHendelse,
        ctx: Transaksjonskontekst.Aktiv,
    ): OppfølgingFerdigstiltHendelse {
        require(hendelse.aksjon is OppfølgingAksjon.OpprettKlage) { "Ugyldig aksjon for opprettKlage: ${hendelse.aksjon}" }
        val aksjon = hendelse.aksjon as OppfølgingAksjon.OpprettKlage

        val klageOppgave =
            klageMediator.opprettKlage(
                klageMottattHendelse =
                    KlageMottattHendelse(
                        ident = oppfølging.person.ident,
                        opprettet = oppfølging.opprettet,
                        journalpostId = null,
                        sakId = aksjon.valgtSakId,
                        utførtAv = hendelse.utførtAv,
                    ),
                ctx = ctx,
            )

        return OppfølgingFerdigstiltHendelse(
            oppfølgingId = oppfølging.id,
            aksjonType = hendelse.aksjon.type,
            opprettetBehandlingId = klageOppgave.behandling.behandlingId,
            utførtAv = hendelse.utførtAv,
        )
    }

    fun opprettBehandling(
        oppfølging: Oppfølging,
        hendelse: FerdigstillOppfølgingHendelse,
    ): OppfølgingFerdigstiltHendelse {
        val saksbehandlerToken =
            when (val aksjon = hendelse.aksjon) {
                is OppfølgingAksjon.OpprettManuellBehandling -> aksjon.saksbehandlerToken
                is OppfølgingAksjon.OpprettRevurderingBehandling -> aksjon.saksbehandlerToken
                is OppfølgingAksjon.OpprettRevurderingBehandlingEtterKlage -> aksjon.saksbehandlerToken
                else -> throw IllegalArgumentException("Ugyldig aksjon for opprettBehandling: $aksjon")
            }

        val hendelseDato = oppfølging.opprettet.toLocalDate()
        val hendelseId = oppfølging.id.toString()

        val opprettBehandlingTypeDTO =
            when (hendelse.aksjon) {
                is OppfølgingAksjon.OpprettManuellBehandling -> {
                    val begrunnelse = oppfølging.vurdering() ?: "Opprettet fra oppfølging"
                    OpprettBehandlingTypeDTO.Manuell(oppfølging.person.ident, hendelseDato, hendelseId, begrunnelse)
                }

                is OppfølgingAksjon.OpprettRevurderingBehandling -> {
                    val begrunnelse = oppfølging.vurdering() ?: "Opprettet fra oppfølging"
                    OpprettBehandlingTypeDTO.Revurdering(oppfølging.person.ident, hendelseDato, hendelseId, begrunnelse)
                }

                is OppfølgingAksjon.OpprettRevurderingBehandlingEtterKlage -> {
                    val klageBehandlingId =
                        requireNotNull(oppfølging.strukturertData["basertPåBehandling"] as? String) {
                            "Oppfølging ${oppfølging.id} mangler basertPåBehandling i strukturertData - kan ikke opprette revurdering etter klage"
                        }.let(UUID::fromString)

                    val klageBehandling = klageMediator.hentKlageBehandlingUtenTilgangssjekk(klageBehandlingId)
                    val klageinstansVedtak =
                        klageBehandling.klageinstansVedtak() as? KlageinstansVedtak.Klage
                            ?: error("Klagebehandling $klageBehandlingId mangler klageinstansvedtak")
                    val kabalReferanse = klageinstansVedtak.id

                    // TODO steg 4: bruk kabalReferanse til å opprette en NyKlage (OmgjøringEtterKlage) i
                    // dp-behandling med kildesystem=Klageinstans istedenfor en plain Revurdering, slik at
                    // dp-behandling selv kjenner referansen til Kabal-vedtaket.
                    val begrunnelse =
                        (oppfølging.vurdering() ?: "Opprettet fra oppfølging") +
                            " (revurdering etter klageinstansvedtak $kabalReferanse, basert på klagebehandling $klageBehandlingId)"
                    OpprettBehandlingTypeDTO.Revurdering(oppfølging.person.ident, hendelseDato, hendelseId, begrunnelse)
                }

                else -> throw IllegalArgumentException("Ugyldig aksjon for opprettBehandling: ${hendelse.aksjon}")
            }

        behandlingKlient
            .opprettBehandling(
                opprettBehandlingTypeDTO = opprettBehandlingTypeDTO,
                saksbehandlerToken = saksbehandlerToken,
            ).let { result ->
                return OppfølgingFerdigstiltHendelse(
                    oppfølgingId = oppfølging.id,
                    aksjonType = hendelse.aksjon.type,
                    opprettetBehandlingId = result.getOrThrow(),
                    utførtAv = hendelse.utførtAv,
                )
            }
    }

    fun opprettNyOppfølging(
        oppfølging: Oppfølging,
        hendelse: FerdigstillOppfølgingHendelse,
        oppfølgingMediator: OppfølgingMediator,
        ctx: Transaksjonskontekst.Aktiv,
    ): OppfølgingFerdigstiltHendelse {
        require(hendelse.aksjon is OppfølgingAksjon.OpprettOppfølging) { "Ugyldig aksjon for opprettNyOppfølging: ${hendelse.aksjon}" }
        val aksjon = hendelse.aksjon as OppfølgingAksjon.OpprettOppfølging

        val nyOppgaveHendelse =
            OpprettOppfølgingHendelse(
                ident = oppfølging.person.ident,
                aarsak = aksjon.aarsak,
                tittel = aksjon.tittel,
                beskrivelse = aksjon.beskrivelse,
                frist = aksjon.frist,
                beholdOppgaven = aksjon.beholdOppgaven,
                utførtAv = hendelse.utførtAv,
            )

        val opprettet = oppfølgingMediator.taImot(nyOppgaveHendelse, ctx)

        return OppfølgingFerdigstiltHendelse(
            oppfølgingId = oppfølging.id,
            aksjonType = aksjon.type,
            opprettetBehandlingId = opprettet.oppfølgingId,
            opprettetOppgaveId = opprettet.oppgaveId,
            utførtAv = hendelse.utførtAv,
        )
    }
}
