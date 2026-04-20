package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class UtløstAvTypeTest {
    companion object {
        @JvmStatic
        fun kjenteTyper(): Stream<Arguments> =
            Stream.of(
                Arguments.of("SØKNAD", UtløstAvType.DpBehandling.Søknad),
                Arguments.of("MELDEKORT", UtløstAvType.DpBehandling.Meldekort),
                Arguments.of("MANUELL", UtløstAvType.DpBehandling.Manuell),
                Arguments.of("REVURDERING", UtløstAvType.DpBehandling.Revurdering),
                Arguments.of("FERIETILLEGG", UtløstAvType.DpBehandling.Ferietillegg),
                Arguments.of("INNSENDING", UtløstAvType.Intern.Innsending),
                Arguments.of("KLAGE", UtløstAvType.Intern.Klage),
                Arguments.of("OPPFØLGING", UtløstAvType.Intern.Oppfølging),
            )

        @JvmStatic
        fun behandletHendelseTyper(): Stream<Arguments> =
            Stream.of(
                Arguments.of("Søknad", UtløstAvType.DpBehandling.Søknad),
                Arguments.of("Meldekort", UtløstAvType.DpBehandling.Meldekort),
                Arguments.of("Manuell", UtløstAvType.DpBehandling.Manuell),
                Arguments.of("Omgjøring", UtløstAvType.DpBehandling.Revurdering),
                Arguments.of("Ferietillegg", UtløstAvType.DpBehandling.Ferietillegg),
            )
    }

    @ParameterizedTest
    @MethodSource("kjenteTyper")
    fun `valueOf skal mappe kjente typer korrekt`(
        name: String,
        forventet: UtløstAvType,
    ) {
        UtløstAvType.valueOf(name) shouldBe forventet
    }

    @ParameterizedTest
    @MethodSource("behandletHendelseTyper")
    fun `fraBehandletHendelseType skal mappe Kafka-typer korrekt`(
        behandletHendelseType: String,
        forventet: UtløstAvType.DpBehandling,
    ) {
        UtløstAvType.DpBehandling.fraBehandletHendelseType(behandletHendelseType) shouldBe forventet
    }

    @Test
    fun `valueOf skal kaste exception for ukjente typer`() {
        shouldThrow<IllegalStateException> {
            UtløstAvType.valueOf("UKJENT")
        }
    }

    @Test
    fun `fraBehandletHendelseType skal kaste exception for ukjent type`() {
        shouldThrow<IllegalStateException> {
            UtløstAvType.DpBehandling.fraBehandletHendelseType("UkjentType")
        }
    }

    @Test
    fun `entries inneholder alle kjente typer`() {
        UtløstAvType.entries.size shouldBe 9
        UtløstAvType.entries.map { it.name }.toSet() shouldBe
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
        UtløstAvType.DpBehandling.Søknad.name shouldBe "SØKNAD"
        UtløstAvType.DpBehandling.Revurdering.name shouldBe "REVURDERING"
        UtløstAvType.Intern.Klage.name shouldBe "KLAGE"
    }

    @Test
    fun `behandletHendelseType returnerer Kafka-verdi`() {
        UtløstAvType.DpBehandling.Søknad.behandletHendelseType shouldBe "Søknad"
        UtløstAvType.DpBehandling.Revurdering.behandletHendelseType shouldBe "Omgjøring"
    }

    @Test
    fun `DpBehandling-typer er DpBehandling`() {
        (UtløstAvType.DpBehandling.Søknad is UtløstAvType.DpBehandling) shouldBe true
        (UtløstAvType.DpBehandling.Ferietillegg is UtløstAvType.DpBehandling) shouldBe true
    }

    @Test
    fun `Intern-typer er ikke DpBehandling`() {
        (UtløstAvType.Intern.Klage is UtløstAvType.DpBehandling) shouldBe false
        (UtløstAvType.Intern.Innsending is UtløstAvType.DpBehandling) shouldBe false
    }
}
