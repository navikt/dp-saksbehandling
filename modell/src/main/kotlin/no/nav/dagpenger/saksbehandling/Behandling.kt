package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val hendelse: Hendelse,
    val utløstAv: HendelseBehandler,
    val oppgaveId: UUID? = null,
) {
    companion object {
        fun rehydrer(
            behandlingId: UUID,
            opprettet: LocalDateTime,
            hendelse: Hendelse,
            utløstAv: HendelseBehandler,
        ) = Behandling(
            behandlingId = behandlingId,
            opprettet = opprettet,
            hendelse = hendelse,
            utløstAv = utløstAv,
        )
    }
}

sealed class HendelseBehandler(
    open val name: String,
) {
    sealed class DpBehandling(
        override val name: String,
        open val behandletHendelseType: String,
    ) : HendelseBehandler(name) {
        data object Søknad : DpBehandling("SØKNAD", "Søknad")

        data object Meldekort : DpBehandling("MELDEKORT", "Meldekort")

        data object Manuell : DpBehandling("MANUELL", "Manuell")

        data object Revurdering : DpBehandling("REVURDERING", "Omgjøring")

        data object Ferietillegg : DpBehandling("FERIETILLEGG", "Ferietillegg")

        data object Arbeidssøkerperiode : DpBehandling("ARBEIDSSØKERPERIODE", "Arbeidssøkerperiode")

        data object Samordning : DpBehandling("SAMORDNING", "Samordning")

        companion object {
            private val behandletHendelseTyper by lazy {
                listOf(Søknad, Meldekort, Manuell, Revurdering, Ferietillegg, Arbeidssøkerperiode, Samordning)
                    .associateBy { it.behandletHendelseType }
            }

            fun fraBehandletHendelseType(type: String): DpBehandling =
                behandletHendelseTyper[type]
                    ?: error("Ukjent behandletHendelseType: '$type'. Legg til nytt data object i DpBehandling.")
        }
    }

    sealed class Intern(
        override val name: String,
    ) : HendelseBehandler(name) {
        data object Innsending : Intern("INNSENDING")

        data object Klage : Intern("KLAGE")

        data object Oppfølging : Intern("OPPFØLGING")
    }

    companion object {
        private val kjente by lazy {
            listOf(
                DpBehandling.Søknad,
                DpBehandling.Meldekort,
                DpBehandling.Manuell,
                DpBehandling.Revurdering,
                DpBehandling.Ferietillegg,
                DpBehandling.Arbeidssøkerperiode,
                DpBehandling.Samordning,
                Intern.Innsending,
                Intern.Klage,
                Intern.Oppfølging,
            ).associateBy { it.name }
        }

        val entries: List<HendelseBehandler> get() = kjente.values.toList()

        fun valueOf(name: String): HendelseBehandler =
            kjente[name]
                ?: error("Ukjent HendelseBehandler: '$name'. Gyldige verdier: ${kjente.keys}")
    }

    override fun toString(): String = name
}
