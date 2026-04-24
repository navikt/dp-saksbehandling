package no.nav.dagpenger.saksbehandling.serder

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import kotlin.reflect.KClass

private val hendelseTyper: Map<String, KClass<out Hendelse>> by lazy {
    finnAlleKonkreteSubklasser(Hendelse::class).associateBy { it.simpleName!! }
}

private fun finnAlleKonkreteSubklasser(klass: KClass<out Hendelse>): List<KClass<out Hendelse>> =
    klass.sealedSubclasses.flatMap { sub ->
        if (sub.isSealed) {
            finnAlleKonkreteSubklasser(sub)
        } else {
            listOf(sub)
        }
    }

internal fun Hendelse.tilJson(): String = objectMapper.writeValueAsString(this)

internal inline fun <reified T : Hendelse> String.tilHendelse(): T = objectMapper.readValue(this, T::class.java)

internal fun rehydrerHendelse(
    hendelseType: String?,
    hendelseJson: String,
): Hendelse {
    if (hendelseType == null) return TomHendelse
    val klass =
        hendelseTyper[hendelseType]
            ?: throw IllegalArgumentException("Ukjent hendelse type: $hendelseType")
    return objectMapper.readValue(hendelseJson, klass.java)
}
