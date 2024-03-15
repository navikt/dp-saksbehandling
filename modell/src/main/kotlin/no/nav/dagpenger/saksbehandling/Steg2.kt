package no.nav.dagpenger.saksbehandling

sealed class Steg2(val beskrivendeId: String, val opplysninger: List<Opplysning>) {
    abstract val tilstand: Steg.Tilstand
    abstract val rotNodeNavn: String

}

abstract class VilkårSteg(beskrivendeId: String, opplysninger: List<Opplysning>) : Steg2(
    beskrivendeId = beskrivendeId,
    opplysninger = opplysninger,
) {
    override val tilstand: Steg.Tilstand
        get() {
            val toppnode = this.opplysninger.single{
                it.navn == this.rotNodeNavn
            }
            return if (toppnode.status.name == OpplysningStatus.Faktum.name &&
                toppnode.dataType == "boolean" &&
                toppnode.verdi == "true"
            ) {
                Steg.Tilstand.OPPFYLT
            } else if (toppnode.status.name == OpplysningStatus.Faktum.name &&
                toppnode.dataType == "boolean" &&
                toppnode.verdi == "false"
            ) {
                Steg.Tilstand.IKKE_OPPFYLT
            } else {
                Steg.Tilstand.MANUELL_BEHANDLING
            }
        }
}