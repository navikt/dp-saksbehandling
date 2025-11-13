package no.nav.dagpenger.saksbehandling.db.klage

import PersonMediator
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.api.Oppslag
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.klage.Datatype
import no.nav.dagpenger.saksbehandling.klage.Klage
import no.nav.dagpenger.saksbehandling.klage.Klage.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PostgresKlageRepositoryTest {
    private val testPerson =
        Person(
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )
    val klageId = UUIDv7.ny()

    private fun setupDBOgSak(test: (PostgresKlageRepository, UUID) -> Unit) {
        withMigratedDb { ds ->
            val sakMediator =
                SakMediator(
                    personMediator =
                        PersonMediator(
                            personRepository =
                                PostgresPersonRepository(
                                    dataSource = ds,
                                ),
                            oppslag =
                                mockk<Oppslag>().also {
                                    coEvery { it.erSkjermetPerson(testPerson.ident) } returns false
                                    coEvery { it.adressebeskyttelseGradering(testPerson.ident) } returns
                                        AdressebeskyttelseGradering.UGRADERT
                                },
                        ),
                    sakRepository = PostgresSakRepository(dataSource = ds),
                )

            val sak =
                sakMediator.opprettSak(
                    søknadsbehandlingOpprettetHendelse =
                        SøknadsbehandlingOpprettetHendelse(
                            ident = testPerson.ident,
                            behandlingId = UUIDv7.ny(),
                            søknadId = UUIDv7.ny(),
                            opprettet = LocalDateTime.now(),
                        ),
                )
            sakMediator.knyttTilSak(
                behandlingOpprettetHendelse =
                    BehandlingOpprettetHendelse(
                        behandlingId = klageId,
                        ident = testPerson.ident,
                        sakId = sak.sakId,
                        opprettet = LocalDateTime.now(),
                        type = UtløstAvType.KLAGE,
                    ),
            )
            val klageRepository = PostgresKlageRepository(datasource = ds)

            test(klageRepository, sak.sakId)
        }
    }

    @Test
    fun `Skal kunne lagre og hente klagebehandlinger`() {
        setupDBOgSak { klageRepository, _ ->

            val klageMottattHendelse =
                KlageMottattHendelse(
                    ident = testPerson.ident,
                    sakId = UUIDv7.ny(),
                    opprettet = LocalDateTime.now(),
                    journalpostId = "journalpostId",
                )

            val klage =
                Klage.rehydrer(
                    behandlingId = klageId,
                    opprettet = klageMottattHendelse.opprettet,
                    journalpostId = "journalpostId",
                    tilstand = Klage.Behandles,
                    behandlendeEnhet = null,
                    tilstandslogg =
                        KlageTilstandslogg(
                            Tilstandsendring(
                                tilstand = BEHANDLES,
                                hendelse = klageMottattHendelse,
                            ),
                        ),
                )

            val boolskOpplysningId =
                klage.finnEnBoolskOpplysning().also {
                    klage.svar(it, Verdi.Boolsk(true))
                }

            val datoOpplysningerId =
                klage.finnEnDatoOpplysningerId().also {
                    klage.svar(it, Verdi.Dato(LocalDate.MIN))
                }

            val listeOpplysning =
                klage.finnEnListeOpplysning().also {
                    klage.svar(it.opplysningId, Verdi.Flervalg(it.valgmuligheter))
                }

            val tekstOpplysningUtenValg =
                klage.finnEnStringOpplysningUtenValg().also {
                    klage.svar(it.opplysningId, Verdi.TekstVerdi("String"))
                }

            val boolskOpplysningMedTomVerdi =
                klage.synligeOpplysninger().first {
                    it.type.datatype == Datatype.BOOLSK && it.verdi() is Verdi.TomVerdi
                }.opplysningId

            klageRepository.lagre(klage)

            val hentetKlageBehandling = klageRepository.hentKlageBehandling(klage.behandlingId)

            hentetKlageBehandling.behandlingId shouldBe klage.behandlingId
            hentetKlageBehandling.journalpostId() shouldBe klage.journalpostId()
            hentetKlageBehandling.tilstand().type shouldBe BEHANDLES
            hentetKlageBehandling.alleOpplysninger() shouldContainExactly klage.alleOpplysninger()

            hentetKlageBehandling.finnEnOpplysning(boolskOpplysningId).verdi() shouldBe Verdi.Boolsk(true)
            hentetKlageBehandling.finnEnOpplysning(datoOpplysningerId).verdi() shouldBe Verdi.Dato(LocalDate.MIN)
            hentetKlageBehandling.finnEnOpplysning(listeOpplysning.opplysningId).verdi() shouldBe
                Verdi.Flervalg(
                    listeOpplysning.valgmuligheter,
                )
            hentetKlageBehandling.finnEnOpplysning(tekstOpplysningUtenValg.opplysningId)
                .verdi() shouldBe Verdi.TekstVerdi("String")
            hentetKlageBehandling.finnEnOpplysning(boolskOpplysningMedTomVerdi).verdi() shouldBe Verdi.TomVerdi

            hentetKlageBehandling.tilstandslogg.size shouldBe klage.tilstandslogg.size
        }
    }

    private fun Klage.finnEnOpplysning(id: UUID): Opplysning {
        return this.alleOpplysninger().single { opplysning -> opplysning.opplysningId == id }
    }

    private fun Klage.finnEnBoolskOpplysning(): UUID {
        return this.synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }.opplysningId
    }

    private fun Klage.finnEnStringOpplysningUtenValg(): Opplysning {
        return this.synligeOpplysninger().first { opplysning ->
            opplysning.type.datatype == Datatype.TEKST && opplysning.valgmuligheter.isEmpty()
        }
    }

    private fun Klage.finnEnDatoOpplysningerId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.opplysningId
    }

    private fun Klage.finnEnListeOpplysning(): Opplysning {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }
    }
}
