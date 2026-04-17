package no.nav.dagpenger.saksbehandling.db.oppfolging

import no.nav.dagpenger.saksbehandling.oppfolging.Oppfølging
import java.util.UUID

interface OppfølgingRepository {
    fun lagre(oppfølging: Oppfølging)

    fun hent(id: UUID): Oppfølging

    fun finn(id: UUID): Oppfølging?

    fun finnForPerson(ident: String): List<Oppfølging>
}
