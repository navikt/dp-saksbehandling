package no.nav.dagpenger.saksbehandling

data class Steg(val beskrivendeId: String, val opplysninger: List<Opplysning>) {
    val tilstand: Tilstand
        get() {
            val toppnode = this.opplysninger.first()
            return if (toppnode.status.name == OpplysningStatus.Faktum.name &&
                toppnode.dataType == "boolean" &&
                toppnode.verdi == "true"
            ) {
                Tilstand.OPPFYLT
            } else if (toppnode.status.name == OpplysningStatus.Faktum.name &&
                toppnode.dataType == "boolean" &&
                toppnode.verdi == "false"
            ) {
                Tilstand.IKKE_OPPFYLT
            } else {
                Tilstand.MANUELL_BEHANDLING
            }
        }

    enum class Tilstand {
        OPPFYLT,
        IKKE_OPPFYLT,
        MANUELL_BEHANDLING,
    }
}
