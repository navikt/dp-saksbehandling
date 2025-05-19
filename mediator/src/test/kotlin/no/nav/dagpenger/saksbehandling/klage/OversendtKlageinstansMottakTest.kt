package no.nav.dagpenger.saksbehandling.klage

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.OversendtKlageinstansHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Type.OVERSEND_KLAGEINSTANS
import org.junit.jupiter.api.Test

class OversendtKlageinstansMottakTest {
    private val testRapid = TestRapid()
    private val fagsakId = UUIDv7.ny()
    private val ident = "11223312345"
    private val klageBehandling = lagKlagebehandling(tilstand = OVERSEND_KLAGEINSTANS, behandlendeEnhet = "4408")
    private val oversendtKlageinstansHendelse =
        OversendtKlageinstansHendelse(behandlingId = klageBehandling.behandlingId)
    private val klageMediatorMock =
        mockk<KlageMediator>().also {
            every { it.hentKlageBehandling(any(), any()) } returns klageBehandling
            every { it.oversendtTilKlageinstans(any()) } just Runs
        }

    @Test
    fun `Skal motta oversendt klageinstans hendelse`() {
        OversendtKlageinstansMottak(
            rapidsConnection = testRapid,
            klageMediator = klageMediatorMock,
        )
        testRapid.sendTestMessage(
            oversendtKlageinstansOk(
                behandlingId = klageBehandling.behandlingId,
                fagsakId = fagsakId,
                ident = ident,
            ),
        )

        verify(exactly = 1) {
            klageMediatorMock.oversendtTilKlageinstans(
                hendelse = oversendtKlageinstansHendelse,
            )
        }
    }
}
