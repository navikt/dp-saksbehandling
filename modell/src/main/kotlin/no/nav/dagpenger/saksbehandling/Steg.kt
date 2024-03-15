package no.nav.dagpenger.saksbehandling

sealed class Steg(val beskrivendeId: String, val opplysninger: List<Opplysning>) {
    abstract val tilstand: Tilstand
    abstract val toppNodeNavn: String
    protected open val toppnode: Opplysning
        get() = opplysninger.single { it.navn == toppNodeNavn }

    enum class Tilstand {
        OPPFYLT,
        IKKE_OPPFYLT,
        MANUELL_BEHANDLING,
    }
}

abstract class VilkårSteg(beskrivendeId: String, opplysninger: List<Opplysning>) : Steg(
    beskrivendeId = beskrivendeId,
    opplysninger = opplysninger,
) {
    override val toppnode: Opplysning
        get() {
            require(super.toppnode.dataType == "boolean") { "Vilkår må være av type boolean" }
            return super.toppnode
        }

    override val tilstand: Tilstand
        get() {
            return if (toppnode.status == OpplysningStatus.Hypotese) {
                Tilstand.MANUELL_BEHANDLING
            } else {
                when (toppnode.verdi) {
                    "true" -> Tilstand.OPPFYLT
                    "false" -> Tilstand.IKKE_OPPFYLT
                    else -> throw IllegalStateException("Ugyldig verdi for vilkårsSteg2: ${toppnode.verdi}")
                }
            }
        }
}
