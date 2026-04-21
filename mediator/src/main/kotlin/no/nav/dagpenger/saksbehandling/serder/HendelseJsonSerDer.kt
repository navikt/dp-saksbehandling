package no.nav.dagpenger.saksbehandling.serder

import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingLåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpplåstHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.DpBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.InnsendingMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.MeldekortbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppfølgingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.RevurderingBehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SkriptHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import kotlin.reflect.KClass

private val hendelseTyper: Map<String, KClass<out Hendelse>> =
    mapOf(
        // Tilstandsmaskin-hendelser
        "AvbruttHendelse" to AvbruttHendelse::class,
        "AvbrytOppgaveHendelse" to AvbrytOppgaveHendelse::class,
        "BehandlingAvbruttHendelse" to BehandlingAvbruttHendelse::class,
        "BehandlingLåstHendelse" to BehandlingLåstHendelse::class,
        "BehandlingOpplåstHendelse" to BehandlingOpplåstHendelse::class,
        "FjernOppgaveAnsvarHendelse" to FjernOppgaveAnsvarHendelse::class,
        "ForslagTilVedtakHendelse" to ForslagTilVedtakHendelse::class,
        "GodkjentBehandlingHendelse" to GodkjentBehandlingHendelse::class,
        "InnsendingFerdigstiltHendelse" to InnsendingFerdigstiltHendelse::class,
        "NesteOppgaveHendelse" to NesteOppgaveHendelse::class,
        "OppfølgingFerdigstiltHendelse" to OppfølgingFerdigstiltHendelse::class,
        "PåVentFristUtgåttHendelse" to PåVentFristUtgåttHendelse::class,
        "ReturnerTilSaksbehandlingHendelse" to ReturnerTilSaksbehandlingHendelse::class,
        "SendTilKontrollHendelse" to SendTilKontrollHendelse::class,
        "SettOppgaveAnsvarHendelse" to SettOppgaveAnsvarHendelse::class,
        "SkriptHendelse" to SkriptHendelse::class,
        "TomHendelse" to TomHendelse::class,
        "UtsettOppgaveHendelse" to UtsettOppgaveHendelse::class,
        "VedtakFattetHendelse" to VedtakFattetHendelse::class,
        // Opprettelses-hendelser
        "DpBehandlingOpprettetHendelse" to DpBehandlingOpprettetHendelse::class,
        "BehandlingOpprettetHendelse" to BehandlingOpprettetHendelse::class,
        "InnsendingMottattHendelse" to InnsendingMottattHendelse::class,
        "OpprettOppfølgingHendelse" to OpprettOppfølgingHendelse::class,
        // Legacy — gammel data i DB
        "SøknadsbehandlingOpprettetHendelse" to SøknadsbehandlingOpprettetHendelse::class,
        "MeldekortbehandlingOpprettetHendelse" to MeldekortbehandlingOpprettetHendelse::class,
        "ManuellBehandlingOpprettetHendelse" to ManuellBehandlingOpprettetHendelse::class,
        "RevurderingBehandlingOpprettetHendelse" to RevurderingBehandlingOpprettetHendelse::class,
    )

internal fun Hendelse.tilJson(): String = objectMapper.writeValueAsString(this)

internal inline fun <reified T : Hendelse> String.tilHendelse(): T = objectMapper.readValue(this, T::class.java)

internal fun rehydrerHendelse(
    hendelseType: String?,
    hendelseJson: String,
): Hendelse {
    if (hendelseType == null) return TomHendelse
    val klass =
        hendelseTyper[hendelseType]
            ?: throw IllegalArgumentException("Ukjent hendelse type: $hendelseType")
    return objectMapper.readValue(hendelseJson, klass.java)
}
