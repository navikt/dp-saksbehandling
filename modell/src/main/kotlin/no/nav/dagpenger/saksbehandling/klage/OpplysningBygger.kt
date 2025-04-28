package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.UUIDv7
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
import no.nav.dagpenger.saksbehandling.klage.OpplysningType.VURDERIG_AV_KLAGEN

object OpplysningBygger {
    val klagenGjelderOpplysningTyper =
        setOf(
            KLAGEN_GJELDER,
            KLAGEN_GJELDER_VEDTAK,
        )

    val fristvurderingOpplysningTyper =
        setOf(
            KLAGEFRIST,
            KLAGE_MOTTATT,
            KLAGEFRIST_OPPFYLT,
        )

    val oversittetFristOpplysningTyper =
        setOf(
            OPPREISNING_OVERSITTET_FRIST,
            OPPREISNING_OVERSITTET_FRIST_BEGRUNNELSE,
        )

    val formkravOpplysningTyper =
        setOf(
            ER_KLAGEN_SKRIFTLIG,
            ER_KLAGEN_UNDERSKREVET,
            KLAGEN_NEVNER_ENDRING,
            RETTSLIG_KLAGEINTERESSE,
        )

    val utfallOpplysningTyper =
        setOf(
            UTFALL,
            VURDERIG_AV_KLAGEN,
        )

    val tilKlageinstansOpplysningTyper =
        setOf(
            HVEM_KLAGER,
            HJEMLER,
            INTERN_MELDING,
        )

    val fullmektigTilKlageinstansOpplysningTyper =
        setOf(
            FULLMEKTIG_NAVN,
            FULLMEKTIG_ADRESSE_1,
            FULLMEKTIG_ADRESSE_2,
            FULLMEKTIG_ADRESSE_3,
            FULLMEKTIG_POSTNR,
            FULLMEKTIG_POSTSTED,
            FULLMEKTIG_LAND,
        )

    fun lagOpplysninger(vararg opplysninger: OpplysningType): Set<Opplysning> = lagOpplysninger(opplysninger.toSet())

    fun lagOpplysninger(opplysninger: Set<OpplysningType> = emptySet()): Set<Opplysning> {
        return opplysninger.map {
            Opplysning(
                id = UUIDv7.ny(),
                type = it,
                verdi = Verdi.TomVerdi,
            )
        }.toSet()
    }
}
