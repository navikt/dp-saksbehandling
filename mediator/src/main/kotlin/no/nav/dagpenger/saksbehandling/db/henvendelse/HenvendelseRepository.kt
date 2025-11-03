package no.nav.dagpenger.saksbehandling.db.henvendelse

import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse

interface HenvendelseRepository {
    fun lagre(henvendelse: Henvendelse)
}
