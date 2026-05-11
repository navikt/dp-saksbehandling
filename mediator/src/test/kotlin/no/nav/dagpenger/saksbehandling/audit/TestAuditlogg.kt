package no.nav.dagpenger.saksbehandling.audit

import no.nav.dagpenger.aktivitetslogg.AuditOperasjon

class TestAuditlogg : Auditlogg {
    data class Hendelse(
        val operasjon: AuditOperasjon,
        val melding: String,
        val ident: String,
        val saksbehandler: String,
    )

    private val _hendelser = mutableListOf<Hendelse>()
    val hendelser: List<Hendelse> get() = _hendelser.toList()

    override fun les(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) {
        _hendelser.add(Hendelse(AuditOperasjon.READ, melding, ident, saksbehandler))
    }

    override fun opprett(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) {
        _hendelser.add(Hendelse(AuditOperasjon.CREATE, melding, ident, saksbehandler))
    }

    override fun oppdater(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) {
        _hendelser.add(Hendelse(AuditOperasjon.UPDATE, melding, ident, saksbehandler))
    }

    override fun slett(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) {
        _hendelser.add(Hendelse(AuditOperasjon.DELETE, melding, ident, saksbehandler))
    }

    fun reset() {
        _hendelser.clear()
    }
}
