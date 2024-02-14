package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

class VurderAvslagPåMinsteinntektHendelse(
    val søknadUUID: UUID,
    meldingsreferanseId: UUID,
    ident: String,
) : PersonHendelse(meldingsreferanseId, ident)
