package no.nav.dagpenger.saksbehandling.db.innsending

import no.nav.dagpenger.saksbehandling.innsending.Innsending
import java.util.UUID

interface InnsendingRepository {
    fun lagre(innsending: Innsending)

    fun hent(innsendingId: UUID): Innsending

    fun finnInnsendingerForPerson(ident: String): List<Innsending>
}
