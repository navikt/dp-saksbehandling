package no.nav.dagpenger.saksbehandling.db.oppfolging

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.oppfolging.Oppfølging
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingAksjon
import org.junit.jupiter.api.Test
import java.util.UUID

class PostgresOppfølgingRepositoryTest {
    private val testPerson =
        Person(
            id = UUIDv7.ny(),
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )

    @Test
    fun `Skal lagre og hente generell oppgave`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val repository = PostgresOppfølgingRepository(ds)

            personRepository.lagre(testPerson)

            val oppgave =
                Oppfølging.opprett(
                    person = testPerson,
                    tittel = "Test oppgave",
                    beskrivelse = "En beskrivelse",
                )

            repository.lagre(oppgave)

            val hentet = repository.hent(oppgave.id)

            hentet.id shouldBe oppgave.id
            hentet.tittel shouldBe "Test oppgave"
            hentet.beskrivelse shouldBe "En beskrivelse"
            hentet.tilstand() shouldBe "BEHANDLES"
            hentet.person.ident shouldBe testPerson.ident
        }
    }

    @Test
    fun `Skal oppdatere generell oppgave ved ferdigstilling`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val repository = PostgresOppfølgingRepository(ds)

            personRepository.lagre(testPerson)

            val oppgave =
                Oppfølging.opprett(
                    person = testPerson,
                    tittel = "Test",
                )

            repository.lagre(oppgave)

            val sakId = UUID.randomUUID()
            oppgave.startFerdigstilling(vurdering = "Min vurdering", valgtSakId = sakId)
            repository.lagre(oppgave)

            val etterStart = repository.hent(oppgave.id)
            etterStart.tilstand() shouldBe "FERDIGSTILL_STARTET"
            etterStart.vurdering() shouldBe "Min vurdering"
            etterStart.valgtSakId() shouldBe sakId

            oppgave.ferdigstill(aksjonType = OppfølgingAksjon.Type.AVSLUTT)
            repository.lagre(oppgave)

            val etterFerdig = repository.hent(oppgave.id)
            etterFerdig.tilstand() shouldBe "FERDIGSTILT"
            etterFerdig.resultat() shouldBe Oppfølging.Resultat.Ingen
        }
    }

    @Test
    fun `Skal lagre og hente oppgave med klage-resultat`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val repository = PostgresOppfølgingRepository(ds)

            personRepository.lagre(testPerson)

            val oppgave =
                Oppfølging.opprett(
                    person = testPerson,
                    tittel = "Klage-oppgave",
                )
            val behandlingId = UUID.randomUUID()

            oppgave.startFerdigstilling(vurdering = "Klage", valgtSakId = null)
            oppgave.ferdigstill(
                aksjonType = OppfølgingAksjon.Type.OPPRETT_KLAGE,
                opprettetBehandlingId = behandlingId,
            )

            repository.lagre(oppgave)

            val hentet = repository.hent(oppgave.id)
            val resultat = hentet.resultat() as Oppfølging.Resultat.Klage
            resultat.behandlingId shouldBe behandlingId
        }
    }

    @Test
    fun `Skal lagre og hente oppgave med RettTilDagpenger-resultat`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val repository = PostgresOppfølgingRepository(ds)

            personRepository.lagre(testPerson)

            val oppgave =
                Oppfølging.opprett(
                    person = testPerson,
                    tittel = "Manuell behandling",
                )
            val behandlingId = UUID.randomUUID()
            val sakId = UUID.randomUUID()

            oppgave.startFerdigstilling(vurdering = "Manuell", valgtSakId = sakId)
            oppgave.ferdigstill(
                aksjonType = OppfølgingAksjon.Type.OPPRETT_MANUELL_BEHANDLING,
                opprettetBehandlingId = behandlingId,
            )

            repository.lagre(oppgave)

            val hentet = repository.hent(oppgave.id)
            val resultat = hentet.resultat() as Oppfølging.Resultat.RettTilDagpenger
            resultat.behandlingId shouldBe behandlingId
            hentet.valgtSakId() shouldBe sakId
        }
    }

    @Test
    fun `Skal finne oppgaver for person`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val repository = PostgresOppfølgingRepository(ds)

            personRepository.lagre(testPerson)

            val oppgave1 =
                Oppfølging.opprett(
                    person = testPerson,
                    tittel = "Oppgave 1",
                )
            val oppgave2 =
                Oppfølging.opprett(
                    person = testPerson,
                    tittel = "Oppgave 2",
                )

            repository.lagre(oppgave1)
            repository.lagre(oppgave2)

            val oppgaver = repository.finnForPerson(testPerson.ident)

            oppgaver.size shouldBe 2
            oppgaver.map { it.tittel } shouldBe listOf("Oppgave 1", "Oppgave 2")
        }
    }

    @Test
    fun `Skal returnere null ved finn med ukjent id`() {
        withMigratedDb { ds ->
            val repository = PostgresOppfølgingRepository(ds)

            val resultat = repository.finn(UUID.randomUUID())

            resultat shouldBe null
        }
    }
}
