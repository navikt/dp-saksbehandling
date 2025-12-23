package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ManuellKlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsendingDistribuert

interface KlageHendelseObserver {
    fun onKlageMottatt(
        hendelse: KlageMottattHendelse,
        klageBehandling: KlageBehandling,
    ) {}

    fun onManuellKlageMottatt(
        hendelse: ManuellKlageMottattHendelse,
        klageBehandling: KlageBehandling,
    ) {}

    fun onBehandlingUtført(
        hendelse: KlageBehandlingUtført,
        klageBehandling: KlageBehandling,
    ) {}

    fun onVedtakDistribuert(
        hendelse: UtsendingDistribuert,
        klageBehandling: KlageBehandling,
    ) {}

    fun onOversendtTilKlageinstans(
        hendelse: OversendtKlageinstansHendelse,
        klageBehandling: KlageBehandling,
    ) {}

    fun onAvbrutt(
        hendelse: AvbruttHendelse,
        klageBehandling: KlageBehandling,
    ) {}
}
