package no.nav.dagpenger.saksbehandling.db.klage

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.Datatype
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand.Type.BEHANDLES
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandsendring
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.Verdi
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class PostgresKlageRepositoryTest {
    @Test
    fun `Skal kunne lagre og hente klagebehandlinger`() {
        withMigratedDb { ds ->
            val klageRepository = PostgresKlageRepository(ds)
            val klageBehandling =
                KlageBehandling(
                    journalpostId = "journalpostId",
                    tilstandslogg =
                        KlageTilstandslogg(
                            KlageTilstandsendring(
                                tilstand = BEHANDLES,
                                hendelse =
                                    KlageMottattHendelse(
                                        ident = "111111888888",
                                        opprettet = LocalDateTime.now(),
                                        journalpostId = "journalpostId",
                                    ),
                            ),
                        ),
                )
            KlageBehandling.rehydrer(
                behandlingId = UUIDv7.ny(),
                journalpostId = "journalpostId",
                tilstand = KlageBehandling.Behandles,
                behandlendeEnhet = null,
            )

            val boolskOpplysningId =
                klageBehandling.finnEnBoolskOpplysning().also {
                    klageBehandling.svar(it, Verdi.Boolsk(true))
                }

            val datoOpplysningerId =
                klageBehandling.finnEnDatoOpplysningerId().also {
                    klageBehandling.svar(it, Verdi.Dato(LocalDate.MIN))
                }

            val listeOpplysning =
                klageBehandling.finnEnListeOpplysning().also {
                    klageBehandling.svar(it.opplysningId, Verdi.Flervalg(it.valgmuligheter))
                }

            val tekstOpplysningUtenValg =
                klageBehandling.finnEnStringOpplysningUtenValg().also {
                    klageBehandling.svar(it.opplysningId, Verdi.TekstVerdi("String"))
                }

            val boolskOpplysningMedTomVerdi =
                klageBehandling.synligeOpplysninger().first {
                    it.type.datatype == Datatype.BOOLSK && it.verdi() is Verdi.TomVerdi
                }.opplysningId

            klageRepository.lagre(klageBehandling)

            val hentetKlageBehandling = klageRepository.hentKlageBehandling(klageBehandling.behandlingId)

            hentetKlageBehandling.behandlingId shouldBe klageBehandling.behandlingId
            hentetKlageBehandling.journalpostId() shouldBe klageBehandling.journalpostId()
            hentetKlageBehandling.tilstand().type shouldBe BEHANDLES
            hentetKlageBehandling.alleOpplysninger() shouldContainExactly klageBehandling.alleOpplysninger()

            hentetKlageBehandling.finnEnOpplysning(boolskOpplysningId).verdi() shouldBe Verdi.Boolsk(true)
            hentetKlageBehandling.finnEnOpplysning(datoOpplysningerId).verdi() shouldBe Verdi.Dato(LocalDate.MIN)
            hentetKlageBehandling.finnEnOpplysning(listeOpplysning.opplysningId).verdi() shouldBe
                Verdi.Flervalg(listeOpplysning.valgmuligheter)
            hentetKlageBehandling.finnEnOpplysning(tekstOpplysningUtenValg.opplysningId).verdi() shouldBe
                Verdi.TekstVerdi("String")
            hentetKlageBehandling.finnEnOpplysning(boolskOpplysningMedTomVerdi).verdi() shouldBe Verdi.TomVerdi

            hentetKlageBehandling.tilstandslogg.size shouldBe klageBehandling.tilstandslogg.size
        }
    }

    private fun KlageBehandling.finnEnOpplysning(id: UUID): Opplysning {
        return this.alleOpplysninger().single { opplysning -> opplysning.opplysningId == id }
    }

    private fun KlageBehandling.finnEnBoolskOpplysning(): UUID {
        return this.synligeOpplysninger()
            .first { opplysning -> opplysning.type.datatype == Datatype.BOOLSK }.opplysningId
    }

    private fun KlageBehandling.finnEnStringOpplysningUtenValg(): Opplysning {
        return this.synligeOpplysninger().first { opplysning ->
            opplysning.type.datatype == Datatype.TEKST && opplysning.valgmuligheter.isEmpty()
        }
    }

    private fun KlageBehandling.finnEnDatoOpplysningerId(): UUID {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.DATO }.opplysningId
    }

    private fun KlageBehandling.finnEnListeOpplysning(): Opplysning {
        return this.synligeOpplysninger().first { opplysning -> opplysning.type.datatype == Datatype.FLERVALG }
    }
}
