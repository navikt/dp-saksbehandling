package no.nav.dagpenger.saksbehandling.generell

import com.fasterxml.jackson.databind.node.NullNode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.util.UUID

class GenerellOppgaveTest {
    private val testPerson =
        Person(
            id = UUIDv7.ny(),
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )

    @Test
    fun `Skal opprette generell oppgave med påkrevde felter`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Test oppgave",
            )

        oppgave.id shouldNotBe null
        oppgave.person shouldBe testPerson
        oppgave.tittel shouldBe "Test oppgave"
        oppgave.beskrivelse shouldBe ""
        oppgave.tilstand() shouldBe "BEHANDLES"
        oppgave.resultat() shouldBe GenerellOppgave.Resultat.Ingen
    }

    @Test
    fun `Skal opprette generell oppgave med alle felter`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Komplett oppgave",
                beskrivelse = "En beskrivelse",
            )

        oppgave.tittel shouldBe "Komplett oppgave"
        oppgave.beskrivelse shouldBe "En beskrivelse"
    }

    @Test
    fun `Skal starte ferdigstilling og endre tilstand`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Test",
            )

        oppgave.startFerdigstilling(
            vurdering = "Min vurdering",
            valgtSakId = null,
        )

        oppgave.tilstand() shouldBe "FERDIGSTILL_STARTET"
        oppgave.vurdering() shouldBe "Min vurdering"
    }

    @Test
    fun `Skal ferdigstille med AVSLUTT aksjon`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Test",
            )

        oppgave.startFerdigstilling(vurdering = "Vurdering", valgtSakId = null)
        oppgave.ferdigstill(aksjonType = GenerellOppgaveAksjon.Type.AVSLUTT)

        oppgave.tilstand() shouldBe "FERDIGSTILT"
        oppgave.resultat() shouldBe GenerellOppgave.Resultat.Ingen
    }

    @Test
    fun `Skal ferdigstille med OPPRETT_KLAGE aksjon`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Test",
            )
        val behandlingId = UUID.randomUUID()

        oppgave.startFerdigstilling(vurdering = "Klage", valgtSakId = null)
        oppgave.ferdigstill(
            aksjonType = GenerellOppgaveAksjon.Type.OPPRETT_KLAGE,
            opprettetBehandlingId = behandlingId,
        )

        oppgave.tilstand() shouldBe "FERDIGSTILT"
        val resultat = oppgave.resultat() as GenerellOppgave.Resultat.Klage
        resultat.behandlingId shouldBe behandlingId
    }

    @Test
    fun `Skal ferdigstille med OPPRETT_MANUELL_BEHANDLING aksjon`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Test",
            )
        val behandlingId = UUID.randomUUID()

        oppgave.startFerdigstilling(vurdering = "Manuell", valgtSakId = UUID.randomUUID())
        oppgave.ferdigstill(
            aksjonType = GenerellOppgaveAksjon.Type.OPPRETT_MANUELL_BEHANDLING,
            opprettetBehandlingId = behandlingId,
        )

        oppgave.tilstand() shouldBe "FERDIGSTILT"
        val resultat = oppgave.resultat() as GenerellOppgave.Resultat.RettTilDagpenger
        resultat.behandlingId shouldBe behandlingId
    }

    @Test
    fun `Skal ferdigstille med OPPRETT_REVURDERING_BEHANDLING aksjon`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Test",
            )
        val behandlingId = UUID.randomUUID()

        oppgave.startFerdigstilling(vurdering = "Revurdering", valgtSakId = UUID.randomUUID())
        oppgave.ferdigstill(
            aksjonType = GenerellOppgaveAksjon.Type.OPPRETT_REVURDERING_BEHANDLING,
            opprettetBehandlingId = behandlingId,
        )

        oppgave.tilstand() shouldBe "FERDIGSTILT"
        val resultat = oppgave.resultat() as GenerellOppgave.Resultat.RettTilDagpenger
        resultat.behandlingId shouldBe behandlingId
    }

    @Test
    fun `Skal feile ved OPPRETT_KLAGE uten behandlingId`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Test",
            )

        oppgave.startFerdigstilling(vurdering = "Klage", valgtSakId = null)

        shouldThrow<IllegalArgumentException> {
            oppgave.ferdigstill(aksjonType = GenerellOppgaveAksjon.Type.OPPRETT_KLAGE)
        }
    }

    @Test
    fun `Skal lagre valgt sakId ved ferdigstilling`() {
        val oppgave =
            GenerellOppgave.opprett(
                person = testPerson,
                tittel = "Test",
            )
        val sakId = UUID.randomUUID()

        oppgave.startFerdigstilling(vurdering = "Med sak", valgtSakId = sakId)

        oppgave.valgtSakId() shouldBe sakId
    }

    @Test
    fun `Skal rehydrere generell oppgave fra database`() {
        val id = UUIDv7.ny()
        val sakId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()

        val oppgave =
            GenerellOppgave.rehydrer(
                id = id,
                person = testPerson,
                tittel = "Rehydrert",
                beskrivelse = "Beskrivelse",
                strukturertData = NullNode.instance,
                opprettet = java.time.LocalDateTime.now(),
                tilstand = "FERDIGSTILT",
                vurdering = "En vurdering",
                resultat = GenerellOppgave.Resultat.RettTilDagpenger(behandlingId),
                valgtSakId = sakId,
            )

        oppgave.id shouldBe id
        oppgave.tilstand() shouldBe "FERDIGSTILT"
        oppgave.vurdering() shouldBe "En vurdering"
        oppgave.valgtSakId() shouldBe sakId
        (oppgave.resultat() as GenerellOppgave.Resultat.RettTilDagpenger).behandlingId shouldBe behandlingId
    }
}
