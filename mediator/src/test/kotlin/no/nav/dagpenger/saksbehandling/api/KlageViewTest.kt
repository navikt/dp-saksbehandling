package no.nav.dagpenger.saksbehandling.api

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.ER_KLAGEN_SKRIFTLIG
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.ER_KLAGEN_UNDERSKREVET
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_1
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_2
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_ADRESSE_3
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_LAND
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_NAVN
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_POSTNR
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.FULLMEKTIG_POSTSTED
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HJEMLER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.HVEM_KLAGER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.INTERN_MELDING
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_GJELDER_VEDTAK
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGEN_NEVNER_ENDRING
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.KLAGE_MOTTATT
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.OPPREISNING_OVERSITTET_FRIST
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.RETTSLIG_KLAGEINTERESSE
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.UTFALL
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.VURDERNIG_AV_KLAGEN
import no.nav.dagpenger.saksbehandling.klage.OpplysningerBygger.lagOpplysninger
import org.junit.jupiter.api.Test

class KlageViewTest {
    private val opplysninger =
        lagOpplysninger(
            setOf(
                ER_KLAGEN_SKRIFTLIG,
                ER_KLAGEN_UNDERSKREVET,
                KLAGEFRIST,
                KLAGEFRIST_OPPFYLT,
                KLAGEN_GJELDER,
                KLAGEN_GJELDER_VEDTAK,
                KLAGEN_NEVNER_ENDRING,
                KLAGE_MOTTATT,
                OPPREISNING_OVERSITTET_FRIST,
                OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE,
                RETTSLIG_KLAGEINTERESSE,
                UTFALL,
                VURDERNIG_AV_KLAGEN,
                HVEM_KLAGER,
                HJEMLER,
                INTERN_MELDING,
                FULLMEKTIG_NAVN,
                FULLMEKTIG_ADRESSE_1,
                FULLMEKTIG_ADRESSE_2,
                FULLMEKTIG_ADRESSE_3,
                FULLMEKTIG_POSTNR,
                FULLMEKTIG_POSTSTED,
                FULLMEKTIG_LAND,
            ),
        )

    @Test
    fun `Riktig  rekkefølge på behandling opplysninger`() {
        KlageView.behandlingOpplysninger(opplysninger.shuffled())
            .map { opplysning -> opplysning.type } shouldBe
            listOf(
                KLAGEN_GJELDER,
                KLAGEN_GJELDER_VEDTAK,
                KLAGEFRIST,
                KLAGE_MOTTATT,
                KLAGEFRIST_OPPFYLT,
                OPPREISNING_OVERSITTET_FRIST,
                OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE,
                ER_KLAGEN_SKRIFTLIG,
                ER_KLAGEN_UNDERSKREVET,
                KLAGEN_NEVNER_ENDRING,
                RETTSLIG_KLAGEINTERESSE,
            )
    }

    @Test
    fun `Riktig rekkefølge på utfallsopplysninger`() {
        KlageView.utfallOpplysninger(opplysninger.shuffled())
            .map { opplysning -> opplysning.type } shouldBe
            listOf(
                UTFALL,
                VURDERNIG_AV_KLAGEN,
                HVEM_KLAGER,
                HJEMLER,
                INTERN_MELDING,
                FULLMEKTIG_NAVN,
                FULLMEKTIG_ADRESSE_1,
                FULLMEKTIG_ADRESSE_2,
                FULLMEKTIG_ADRESSE_3,
                FULLMEKTIG_POSTNR,
                FULLMEKTIG_POSTSTED,
                FULLMEKTIG_LAND,
            )
    }
}
