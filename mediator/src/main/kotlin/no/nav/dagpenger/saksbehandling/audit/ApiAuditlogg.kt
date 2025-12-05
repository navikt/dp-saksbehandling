package no.nav.dagpenger.saksbehandling.audit

import com.github.navikt.tbd_libs.rapids_and_rivers_api.FailedMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.SentMessage
import no.nav.dagpenger.aktivitetslogg.Aktivitetslogg
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse
import no.nav.dagpenger.aktivitetslogg.AuditOperasjon
import no.nav.dagpenger.aktivitetslogg.IAktivitetslogg
import no.nav.dagpenger.aktivitetslogg.SpesifikkKontekst
import no.nav.dagpenger.saksbehandling.AktivitetsloggMediator
import no.nav.dagpenger.saksbehandling.UUIDv7

internal class ApiAuditlogg(
    private val aktivitetsloggMediator: AktivitetsloggMediator,
    private val rapidsConnection: MessageContext,
) : Auditlogg {
    override fun les(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) = aktivitetslogg(ident) {
        info(melding, ident, saksbehandler, AuditOperasjon.READ)
    }

    override fun opprett(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) = aktivitetslogg(ident) {
        info(melding, ident, saksbehandler, AuditOperasjon.CREATE)
    }

    override fun oppdater(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) = aktivitetslogg(ident) {
        info(melding, ident, saksbehandler, AuditOperasjon.UPDATE)
    }

    override fun slett(
        melding: String,
        ident: String,
        saksbehandler: String,
    ) = aktivitetslogg(ident) {
        info(melding, ident, saksbehandler, AuditOperasjon.DELETE)
    }

    private fun aktivitetslogg(
        ident: String,
        block: AuditLoggHendelse.() -> Unit,
    ) {
        val aktivitetslogg = AuditLoggHendelse(ident)
        block(aktivitetslogg)
        aktivitetsloggMediator.håndter(KlageAuditContext(rapidsConnection, ident), aktivitetslogg)
    }

    private class KlageAuditContext(
        val rapid: MessageContext,
        val ident: String,
    ) : MessageContext {
        override fun publish(message: String) {
            publish(ident, message)
        }

        override fun publish(
            key: String,
            message: String,
        ) {
            rapid.publish(ident, message)
        }

        override fun publish(messages: List<OutgoingMessage>): Pair<List<SentMessage>, List<FailedMessage>> = rapid.publish(messages)

        override fun rapidName() = "apiAuditlogg"
    }

    private class AuditLoggHendelse(
        val ident: String,
        private val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    ) : AktivitetsloggHendelse,
        IAktivitetslogg by aktivitetslogg {
        override fun ident() = ident

        override fun meldingsreferanseId() = UUIDv7.ny()

        override fun toSpesifikkKontekst() = SpesifikkKontekst("Auditlogg", mapOf("ident" to ident))
    }
}
