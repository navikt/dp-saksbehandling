package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.KontrollertBrev.IKKE_RELEVANT
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.DP_SAK
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.INGEN
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligEndringAvKildeForMeldingOmVedtak
import no.nav.dagpenger.saksbehandling.OppgaveTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.hendelser.EndreMeldingOmVedtakKildeHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class EndreMeldingOmVedtakKildeTest {
    val oppgaveId = UUIDv7.ny()

    private val saksbehandler = Saksbehandler("saksbehandler", grupper = emptySet())

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Ulovlige endring av kilde for melding om vedtak`(tilstandstype: Type) {
        val oppgave = lagOppgave(tilstandType = tilstandstype, saksbehandler)

        if (tilstandstype != UNDER_BEHANDLING) {
            shouldThrow<UlovligEndringAvKildeForMeldingOmVedtak> {
                oppgave.endreMeldingOmVedtakKilde(
                    endreMeldingOmVedtakKildeHendelse =
                        EndreMeldingOmVedtakKildeHendelse(
                            oppgaveId = oppgaveId,
                            meldingOmVedtakKilde = GOSYS,
                            utførtAv = saksbehandler,
                        ),
                )
            }
        }
    }

    @Test
    fun `Skal kunne endre kilde for melding om vedtak i tilstand UNDER_BEHANDLING`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler)

        shouldNotThrowAny {
            oppgave.endreMeldingOmVedtakKilde(
                endreMeldingOmVedtakKildeHendelse =
                    EndreMeldingOmVedtakKildeHendelse(
                        oppgaveId = oppgaveId,
                        meldingOmVedtakKilde = GOSYS,
                        utførtAv = saksbehandler,
                    ),
            )
            oppgave.kontrollertBrev() shouldBe IKKE_RELEVANT
            oppgave.endreMeldingOmVedtakKilde(
                endreMeldingOmVedtakKildeHendelse =
                    EndreMeldingOmVedtakKildeHendelse(
                        oppgaveId = oppgaveId,
                        meldingOmVedtakKilde = INGEN,
                        utførtAv = saksbehandler,
                    ),
            )
            oppgave.kontrollertBrev() shouldBe IKKE_RELEVANT
            oppgave.endreMeldingOmVedtakKilde(
                endreMeldingOmVedtakKildeHendelse =
                    EndreMeldingOmVedtakKildeHendelse(
                        oppgaveId = oppgaveId,
                        meldingOmVedtakKilde = DP_SAK,
                        utførtAv = saksbehandler,
                    ),
            )
            oppgave.kontrollertBrev() shouldBe IKKE_RELEVANT
        }
    }
}
