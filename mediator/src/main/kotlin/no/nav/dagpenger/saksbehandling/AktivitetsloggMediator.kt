package no.nav.dagpenger.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggEventMapper
import no.nav.dagpenger.aktivitetslogg.AktivitetsloggHendelse

internal class AktivitetsloggMediator {
    private val aktivitetsloggEventMapper = AktivitetsloggEventMapper()

    @WithSpan
    fun håndter(
        context: MessageContext,
        hendelse: AktivitetsloggHendelse,
    ) {
        Span.current().addEvent("Publiserer aktivitetslogg")
        aktivitetsloggEventMapper.håndter(hendelse) { aktivitetLoggMelding ->
            context.publish(
                JsonMessage
                    .newMessage(
                        aktivitetLoggMelding.eventNavn,
                        aktivitetLoggMelding.innhold,
                    ).toJson(),
            )
        }
    }
}
