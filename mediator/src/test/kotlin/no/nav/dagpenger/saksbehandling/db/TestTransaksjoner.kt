package no.nav.dagpenger.saksbehandling.db

import io.mockk.every
import io.mockk.mockk

/**
 * Returnerer en [Transaksjoner] for enhetstester uten database. Kjører transaksjonsblokken
 * direkte med en relaxed [Transaksjonskontekst.Aktiv], slik at mediator-logikk som lagrer via
 * mockede repositories og sender via [no.nav.dagpenger.saksbehandling.utboks.TestUtboks]
 * eksekveres synkront.
 *
 * Bruk ekte `Transaksjoner(DatabaseSession(dataSource))` i tester som faktisk skal persistere.
 */
fun kjørendeTransaksjoner(): Transaksjoner {
    val aktiv = mockk<Transaksjonskontekst.Aktiv>(relaxed = true)
    return mockk {
        every { transaksjon(block = any<(Transaksjonskontekst.Aktiv) -> Any?>()) } answers {
            firstArg<(Transaksjonskontekst.Aktiv) -> Any?>().invoke(aktiv)
        }
        every {
            transaksjon(ctx = any(), block = any<(Transaksjonskontekst.Aktiv) -> Any?>())
        } answers {
            secondArg<(Transaksjonskontekst.Aktiv) -> Any?>().invoke(aktiv)
        }
    }
}
