package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Saksbehandler
import java.time.LocalDateTime
import java.util.UUID

class OpprettKlageHendelse(
    val henvendelseId: UUID,
    val ident: String,
    val mottatt: LocalDateTime,
    val journalpostId: String,
    val sakId: UUID,
    override val utførtAv: Saksbehandler,
) : Hendelse(utførtAv)
