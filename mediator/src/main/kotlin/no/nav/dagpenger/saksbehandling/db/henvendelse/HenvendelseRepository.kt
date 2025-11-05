package no.nav.dagpenger.saksbehandling.db.henvendelse

import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse
import java.util.UUID

interface HenvendelseRepository {
    fun lagre(henvendelse: Henvendelse)

    fun hent(henvendelseId: UUID): Henvendelse

    fun finnHenvendelserForPerson(ident: String): List<Henvendelse>
}
