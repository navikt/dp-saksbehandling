package no.nav.dagpenger.saksbehandling.db.klage

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilJson
import no.nav.dagpenger.saksbehandling.db.klage.KlageOpplysningerMapper.tilKlageOpplysninger
import no.nav.dagpenger.saksbehandling.klage.KlagenGjelderType
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAKSDATO
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_NEVNER_ENDRING
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGE_MOTTATT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.OPPREISNING_OVERSITTET_FRIST
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE
import no.nav.dagpenger.saksbehandling.klage.UgyldigOpplysningException
import no.nav.dagpenger.saksbehandling.klage.Verdi
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KlageOpplysningMapperTest {
    @Test
    fun `Ved  serialisere og deserialisere av opplysniger bevares også regler`() {
        val opplysninger =
            setOf(
                Opplysning(
                    type = KLAGEN_GJELDER_VEDTAKSDATO,
                    verdi = Verdi.TomVerdi,
                ),
            )

        val json =
            shouldNotThrowAny {
                opplysninger.tilJson()
            }

        json.tilKlageOpplysninger().let { deserialiserteOpplysninger ->
            deserialiserteOpplysninger shouldContainExactly opplysninger

            deserialiserteOpplysninger.single().let {
                it.verdi() shouldBe Verdi.TomVerdi
                shouldNotThrowAny { it.svar(Verdi.Dato(LocalDate.MIN)) }
                shouldThrow<UgyldigOpplysningException> { it.svar(Verdi.Dato(LocalDate.MAX)) }
            }
        }
    }

    @Test
    fun `Skal kunne serialisere og deserialisere opplysniger til json`() {
        val opplysninger =
            setOf(
                Opplysning(
                    type = OPPREISNING_OVERSITTET_FRIST,
                    verdi = Verdi.Boolsk(false),
                ),
                Opplysning(
                    type = OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE,
                    verdi = Verdi.TekstVerdi("Test"),
                ),
                Opplysning(
                    type = KLAGE_MOTTATT,
                    verdi = Verdi.Dato(LocalDate.MIN),
                ),
                Opplysning(
                    type = KLAGEN_GJELDER,
                    verdi = Verdi.Flervalg(listOf(KlagenGjelderType.AVSLAG_PÅ_SØKNAD.name, KlagenGjelderType.ANNET.name)),
                    valgmuligheter = KlagenGjelderType.entries.map { it.name },
                ),
                Opplysning(
                    type = KLAGEN_NEVNER_ENDRING,
                    verdi = Verdi.TomVerdi,
                ),
            )

        val json =
            shouldNotThrowAny {
                opplysninger.tilJson()
            }

        json.tilKlageOpplysninger().let { deserialiserteOpplysninger ->
            deserialiserteOpplysninger shouldContainExactly opplysninger

            deserialiserteOpplysninger.single { it.type == OPPREISNING_OVERSITTET_FRIST }
                .verdi() shouldBe Verdi.Boolsk(false)

            deserialiserteOpplysninger.single { it.type == OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE }
                .verdi() shouldBe Verdi.TekstVerdi("Test")

            deserialiserteOpplysninger.single { it.type == KLAGE_MOTTATT }
                .verdi() shouldBe Verdi.Dato(LocalDate.MIN)

            val klagenGjelder = deserialiserteOpplysninger.single { it.type == KLAGEN_GJELDER }
            klagenGjelder.verdi() shouldBe
                Verdi.Flervalg(
                    KlagenGjelderType.AVSLAG_PÅ_SØKNAD.name,
                    KlagenGjelderType.ANNET.name,
                )
            klagenGjelder.valgmuligheter shouldBe KlagenGjelderType.entries.map { it.name }

            deserialiserteOpplysninger.single { it.type == KLAGEN_NEVNER_ENDRING }
                .verdi() shouldBe Verdi.TomVerdi
        }
    }
}
