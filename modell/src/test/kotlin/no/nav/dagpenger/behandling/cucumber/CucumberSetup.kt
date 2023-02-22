package no.nav.dagpenger.behandling.cucumber

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.cucumber.java8.No
import java.lang.reflect.Type

class CucumberSetup : No {
    companion object {
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    init {
        DefaultParameterTransformer { fromValue: String?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType)
            )
        }
        DefaultDataTableCellTransformer { fromValue: String?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType)
            )
        }
        DefaultDataTableEntryTransformer { fromValue: Map<String?, String?>?, toValueType: Type? ->
            objectMapper.convertValue(
                fromValue,
                objectMapper.constructType(toValueType)
            )
        }
    }
}
