package no.nav.dagpenger.saksbehandling.modell.helpers

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.Behandles
import no.nav.dagpenger.saksbehandling.klage.KlageBehandling.KlageTilstand
import no.nav.dagpenger.saksbehandling.klage.KlageTilstandslogg
import no.nav.dagpenger.saksbehandling.klage.Opplysning
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.Steg
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import java.time.LocalDateTime
import java.util.UUID

object TestHelpers {
    object Sak {
        val sakId: UUID = UUID.randomUUID()
    }

    object Person {
        val personIdent = "41952264877"
        val personId = UUID.fromString("019a2f67-aea7-7b29-84d5-56db0b1dc48a")

        fun lagPerson(
            ident: String = personIdent,
            id: UUID = personId,
            addresseBeskyttelseGradering: AdressebeskyttelseGradering = UGRADERT,
            skjermesSomEgneAnsatte: Boolean = false,
        ) = Person(
            id = id,
            ident = ident,
            skjermesSomEgneAnsatte = skjermesSomEgneAnsatte,
            adressebeskyttelseGradering = addresseBeskyttelseGradering,
        )
    }

    object Klage {
        fun lagKlageBehandling(
            behandlingId: UUID = UUIDv7.ny(),
            opprettet: LocalDateTime = LocalDateTime.now(),
            opplysninger: Set<Opplysning> = setOf(),
            tilstand: KlageTilstand = Behandles,
            journalpostId: String? = null,
            behandlendeEnhet: String? = null,
            tilstandslogg: KlageTilstandslogg =
                KlageTilstandslogg().also {
                    it.leggTil(
                        KlageTilstand.Type.BEHANDLES,
                        KlageMottattHendelse(
                            ident = Person.personIdent,
                            opprettet = LocalDateTime.now(),
                            journalpostId = "klageJournalpostId",
                            sakId = Sak.sakId,
                        ),
                    )
                },
            steg: List<Steg> = emptyList(),
        ): KlageBehandling =
            KlageBehandling.rehydrer(
                behandlingId = behandlingId,
                opprettet = opprettet,
                opplysninger = opplysninger,
                tilstand = tilstand,
                journalpostId = journalpostId,
                behandlendeEnhet = behandlendeEnhet,
                tilstandslogg = tilstandslogg,
                steg = steg,
            )

        fun lagKlageBehandlingMedUtfall(
            tilstand: KlageTilstand = KlageBehandling.BehandlingUtført,
            utfallType: UtfallType,
        ): KlageBehandling =
            lagKlageBehandling(
                tilstand = tilstand,
                opplysninger =
                    setOf(
                        Opplysning(
                            type = OpplysningType.UTFALL,
                            verdi = Verdi.TekstVerdi(utfallType.tekst),
                        ),
                    ),
            )
    }
}
