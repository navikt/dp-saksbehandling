package no.nav.dagpenger.saksbehandling

sealed class Steg(val beskrivendeId: String, val opplysninger: List<Opplysning>) {
    abstract val tilstand: Tilstand
    abstract val toppNodeNavn: String
    abstract val toppnode: Opplysning

    enum class Tilstand {
        OPPFYLT,
        IKKE_OPPFYLT,
        MANUELL_BEHANDLING,
    }
}

abstract class VilkårSteg(beskrivendeId: String, opplysninger: List<Opplysning>, override val toppNodeNavn: String) :
    Steg(
        beskrivendeId = beskrivendeId,
        opplysninger = opplysninger,
    ) {
    override val toppnode: Opplysning =
        opplysninger.singleOrNull { it.navn == toppNodeNavn && it.dataType == "boolean" }
            ?: throw IllegalStateException("Mangler toppnode med navn $toppNodeNavn og dataType boolean")

    override val tilstand: Tilstand
        get() {
            return if (toppnode.status == OpplysningStatus.Hypotese) {
                Tilstand.MANUELL_BEHANDLING
            } else {
                when (toppnode.verdi) {
                    "true" -> Tilstand.OPPFYLT
                    "false" -> Tilstand.IKKE_OPPFYLT
                    else -> throw IllegalStateException("Ugyldig verdi for vilkårsSteg: ${toppnode.verdi}")
                }
            }
        }
}
