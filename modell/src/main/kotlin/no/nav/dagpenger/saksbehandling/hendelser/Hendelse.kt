package no.nav.dagpenger.saksbehandling.hendelser

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.saksbehandling.Aktør

sealed class Hendelse(open val utførtAv: Aktør) {
    companion object {
        inline fun <reified T: Hendelse> mikkemus(json: String): T {
            return objectMapper.readTree()
        }
    }
}

sealed class AnsvarHendelse(utførtAv: Aktør, open val ansvarligIdent: String?) : Hendelse(utførtAv)

data object TomHendelse : Hendelse(Aktør.System.dpSaksbehandling) {
    fun tilJson(): String = "{}"
}

internal val objectMapper: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .enable(SerializationFeature.INDENT_OUTPUT)