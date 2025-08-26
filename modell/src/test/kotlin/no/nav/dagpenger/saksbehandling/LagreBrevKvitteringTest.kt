package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.OppgaveTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.hendelser.LagreBrevKvitteringHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class LagreBrevKvitteringTest {
    val oppgaveId = UUIDv7.ny()

    private val saksbehandler = Saksbehandler("saksbehandler", grupper = emptySet(), tilganger = setOf(TilgangType.BESLUTTER))

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Ulovlig endring av brevkontroll for melding om vedtak`(tilstandstype: Type) {
        val oppgave = lagOppgave(tilstandType = tilstandstype, saksbehandler)

        if (tilstandstype != UNDER_KONTROLL) {
            shouldThrow<RuntimeException> {
                oppgave.lagreBrevKvittering(
                    lagreBrevKvitteringHendelse =
                        LagreBrevKvitteringHendelse(
                            oppgaveId = oppgaveId,
                            kontrollertBrev = Oppgave.KontrollertBrev.JA,
                            utførtAv = saksbehandler,
                        ),
                )
            }
        }
    }

    @Test
    fun `Skal kunne endre brevkontroll for melding om vedtak i tilstand UNDER_KONTROLL`() {
        val oppgave =
            lagOppgave(
                UNDER_KONTROLL,
                saksbehandler,
                meldingOmVedtakKilde =
                    Oppgave.MeldingOmVedtak(
                        kilde = Oppgave.MeldingOmVedtakKilde.GOSYS,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.NEI,
                    ),
            )

        shouldNotThrowAny {
            oppgave.lagreBrevKvittering(
                lagreBrevKvitteringHendelse =
                    LagreBrevKvitteringHendelse(
                        oppgaveId = oppgaveId,
                        kontrollertBrev = Oppgave.KontrollertBrev.JA,
                        utførtAv = saksbehandler,
                    ),
            )
        }
        oppgave.kontrollertBrev() shouldBe Oppgave.KontrollertBrev.JA

        shouldThrow<RuntimeException> {
            oppgave.lagreBrevKvittering(
                lagreBrevKvitteringHendelse =
                    LagreBrevKvitteringHendelse(
                        oppgaveId = oppgaveId,
                        kontrollertBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                        utførtAv = saksbehandler,
                    ),
            )
        }
    }
}
