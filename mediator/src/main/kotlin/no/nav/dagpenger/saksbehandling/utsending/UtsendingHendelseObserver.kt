package no.nav.dagpenger.saksbehandling.utsending

import no.nav.dagpenger.saksbehandling.utsending.hendelser.ArkiverbartBrevHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.DistribuertHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.JournalførtHendelse
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse

interface UtsendingHendelseObserver {
    fun onStartUtsending(
        hendelse: StartUtsendingHendelse,
        utsending: Utsending,
    ) {}

    fun onArkiverbartBrev(
        hendelse: ArkiverbartBrevHendelse,
        utsending: Utsending,
    ) {}

    fun onJournalført(
        hendelse: JournalførtHendelse,
        utsending: Utsending,
    ) {}

    fun onDistribuert(
        hendelse: DistribuertHendelse,
        utsending: Utsending,
    ) {}
}
