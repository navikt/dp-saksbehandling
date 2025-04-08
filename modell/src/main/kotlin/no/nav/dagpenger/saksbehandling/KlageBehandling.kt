package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.Opplysning.Datatype
import no.nav.dagpenger.saksbehandling.OpplysningType.ER_KLAGEN_SKRIFTLIG
import no.nav.dagpenger.saksbehandling.OpplysningType.ER_KLAGEN_UNDERSKREVET
import no.nav.dagpenger.saksbehandling.OpplysningType.KLAGEFRIST_OPPFYLT
import no.nav.dagpenger.saksbehandling.OpplysningType.KLAGE_FRIST
import no.nav.dagpenger.saksbehandling.OpplysningType.KLAGE_MOTTATT
import no.nav.dagpenger.saksbehandling.Utfall.TomtUtfall
import java.time.LocalDate
import java.util.UUID

enum class OpplysningType(
    val navn: String,
    val datatype: Datatype,
) {
    ER_KLAGEN_SKRIFTLIG(
        navn = "Er klagen skriftlig",
        datatype = Datatype.BOOLSK,
    ),

    ER_KLAGEN_UNDERSKREVET(
        navn = "Er klagen underskrevet",
        datatype = Datatype.BOOLSK,
    ),

    KLAGEN_GJELDER(
        navn = "Hva klagen gjelder",
        datatype = Datatype.FLERVALG,
    ),

    KLAGE_FRIST(
        navn = "Frist for å klage",
        datatype = Datatype.DATO,
    ),

    KLAGE_MOTTATT(
        navn = "Klage mottatt",
        datatype = Datatype.DATO,
    ),

    KLAGEFRIST_OPPFYLT(
        navn = "Har klager klaget innen fristen",
        datatype = Datatype.BOOLSK,
    ),

    FRIST_SAKSBEHANDLERS_BEGRUNNELSE(
        navn = "Saksbehandlerens begrunnelse for frist",
        datatype = Datatype.TEKST,
    ),
}

object OpplysningerBygger {
    val formkravOpplysningTyper =
        setOf(
            ER_KLAGEN_SKRIFTLIG,
            ER_KLAGEN_UNDERSKREVET,
        )

    val fristvurderingOpplysningTyper =
        setOf(
            KLAGE_FRIST,
            KLAGE_MOTTATT,
            KLAGEFRIST_OPPFYLT,
        )

    val opplysninger =
        setOf(
            ER_KLAGEN_SKRIFTLIG,
            ER_KLAGEN_UNDERSKREVET,
            OpplysningType.KLAGEN_GJELDER,
            OpplysningType.FRIST_SAKSBEHANDLERS_BEGRUNNELSE,
        )

    fun lagOpplysninger(opplysninger: Set<OpplysningType> = emptySet()): Set<Opplysning> {
        return opplysninger.map {
            Opplysning(
                id = UUID.randomUUID(),
                type = it,
                verdi = Verdi.TomVerdi,
            )
        }.toSet()
    }
}

class KlageBehandling(
    val id: UUID,
    val person: Person,
    private val opplysninger: Set<Opplysning> = OpplysningerBygger.lagOpplysninger(),
    private val steg: LinkedHashSet<Steg> = linkedSetOf<Steg>(),
) {
    private var _utfall: Utfall = TomtUtfall

    val utfall: Utfall get() = _utfall

    fun hentOpplysninger(): Set<Opplysning> {
        return opplysninger
    }

    fun settUtfall(utfall: Utfall) {
        this._utfall = utfall
    }

    fun hentUtfallOpplysninger(): Set<Opplysning> {
        if (utfall in setOf(Utfall.Opprettholdelse)) {
        }
        return if (utfall == TomtUtfall) {
            emptySet()
        } else {
            emptySet()
        }
    }

    fun svar(
        opplysninngId: UUID,
        svar: Boolean,
    ) {
        hentOpplysning(opplysninngId).also { opplysning ->
            opplysning.svar(svar)
            this.revurderUtfall()
        }
    }

    fun svar(
        opplysninngId: UUID,
        svar: String,
    ) {
        hentOpplysning(opplysninngId).also { opplysning ->
            opplysning.svar(svar)
            this.revurderUtfall()
        }
    }

    fun svar(
        opplysninngId: UUID,
        svar: LocalDate,
    ) {
        hentOpplysning(opplysninngId).also { opplysning ->
            opplysning.svar(svar)
            this.revurderUtfall()
        }
    }

    fun svar(
        opplysninngId: UUID,
        svar: List<String>,
    ) {
        hentOpplysning(opplysninngId).also { opplysning ->
            opplysning.svar(svar)
            this.revurderUtfall()
        }
    }

    fun hentOpplysning(opplysningId: UUID): Opplysning {
        return opplysninger.singleOrNull { it.id == opplysningId }
            ?: throw IllegalArgumentException("Fant ikke opplysning med id $opplysningId")
    }

    private fun revurderUtfall() {
        opplysninger.filter { it.type in setOf(ER_KLAGEN_UNDERSKREVET, ER_KLAGEN_SKRIFTLIG) }
            .filter { it.verdi is Verdi.Boolsk }
            .any { !(it.verdi as Verdi.Boolsk).value }
            .let {
                when (it) {
                    true -> this._utfall = Utfall.Avvist
                    false -> {}
                }
            }
    }

    fun hentSteg(): List<Steg> = this.steg.toList()
}

sealed class Verdi {
    data object TomVerdi : Verdi()

    data class TekstVerdi(val value: String) : Verdi()

    data class Dato(val value: LocalDate) : Verdi()

    data class Boolsk(val value: Boolean) : Verdi()

    data class Flervalg(val value: List<String>) : Verdi()
}

sealed class Utfall {
    data object TomtUtfall : Utfall()

    data object Avvist : Utfall()

    data object Opprettholdelse : Utfall()
}
