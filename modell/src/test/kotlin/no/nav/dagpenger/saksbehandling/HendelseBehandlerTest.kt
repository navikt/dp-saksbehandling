package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class HendelseBehandlerTest {
    companion object {
        @JvmStatic
        fun kjenteTyper(): Stream<Arguments> =
            Stream.of(
                Arguments.of("SØKNAD", HendelseBehandler.DpBehandling.Søknad),
                Arguments.of("MELDEKORT", HendelseBehandler.DpBehandling.Meldekort),
                Arguments.of("MANUELL", HendelseBehandler.DpBehandling.Manuell),
                Arguments.of("REVURDERING", HendelseBehandler.DpBehandling.Revurdering),
                Arguments.of("FERIETILLEGG", HendelseBehandler.DpBehandling.Ferietillegg),
                Arguments.of("INNSENDING", HendelseBehandler.Intern.Innsending),
                Arguments.of("KLAGE", HendelseBehandler.Intern.Klage),
                Arguments.of("OPPFØLGING", HendelseBehandler.Intern.Oppfølging),
            )

        @JvmStatic
        fun behandletHendelseTyper(): Stream<Arguments> =
            Stream.of(
                Arguments.of("Søknad", HendelseBehandler.DpBehandling.Søknad),
                Arguments.of("Meldekort", HendelseBehandler.DpBehandling.Meldekort),
                Arguments.of("Manuell", HendelseBehandler.DpBehandling.Manuell),
                Arguments.of("Omgjøring", HendelseBehandler.DpBehandling.Revurdering),
                Arguments.of("Ferietillegg", HendelseBehandler.DpBehandling.Ferietillegg),
                Arguments.of("Arbeidssøkerperiode", HendelseBehandler.DpBehandling.Arbeidssøkerperiode),
            )
    }

    @ParameterizedTest
    @MethodSource("kjenteTyper")
    fun `valueOf skal mappe kjente typer korrekt`(
        name: String,
        forventet: HendelseBehandler,
    ) {
        HendelseBehandler.valueOf(name) shouldBe forventet
    }

    @ParameterizedTest
    @MethodSource("behandletHendelseTyper")
    fun `fraBehandletHendelseType skal mappe Kafka-typer korrekt`(
        behandletHendelseType: String,
        forventet: HendelseBehandler.DpBehandling,
    ) {
        HendelseBehandler.DpBehandling.fraBehandletHendelseType(behandletHendelseType) shouldBe forventet
    }

    @Test
    fun `valueOf skal kaste exception for ukjente typer`() {
        shouldThrow<IllegalStateException> {
            HendelseBehandler.valueOf("UKJENT")
        }
    }

    @Test
    fun `fraBehandletHendelseType skal kaste exception for ukjent type`() {
        shouldThrow<IllegalStateException> {
            HendelseBehandler.DpBehandling.fraBehandletHendelseType("UkjentType")
        }
    }

    @Test
    fun `entries inneholder alle kjente typer`() {
        HendelseBehandler.entries.size shouldBe 9
        HendelseBehandler.entries.map { it.name }.toSet() shouldBe
            setOf(
                "SØKNAD",
                "MELDEKORT",
                "MANUELL",
                "REVURDERING",
                "FERIETILLEGG",
                "ARBEIDSSØKERPERIODE",
                "INNSENDING",
                "KLAGE",
                "OPPFØLGING",
            )
    }

    @Test
    fun `name returnerer UPPERCASE verdi`() {
        HendelseBehandler.DpBehandling.Søknad.name shouldBe "SØKNAD"
        HendelseBehandler.DpBehandling.Revurdering.name shouldBe "REVURDERING"
        HendelseBehandler.Intern.Klage.name shouldBe "KLAGE"
    }

    @Test
    fun `behandletHendelseType returnerer Kafka-verdi`() {
        HendelseBehandler.DpBehandling.Søknad.behandletHendelseType shouldBe "Søknad"
        HendelseBehandler.DpBehandling.Revurdering.behandletHendelseType shouldBe "Omgjøring"
    }

    @Test
    fun `DpBehandling-typer er DpBehandling`() {
        (HendelseBehandler.DpBehandling.Søknad is HendelseBehandler.DpBehandling) shouldBe true
        (HendelseBehandler.DpBehandling.Ferietillegg is HendelseBehandler.DpBehandling) shouldBe true
    }

    @Test
    fun `Intern-typer er ikke DpBehandling`() {
        (HendelseBehandler.Intern.Klage is HendelseBehandler.DpBehandling) shouldBe false
        (HendelseBehandler.Intern.Innsending is HendelseBehandler.DpBehandling) shouldBe false
    }
}
