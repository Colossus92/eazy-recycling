package nl.eazysoftware.eazyrecyclingservice.config.clock

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import kotlinx.datetime.LocalDateTime
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Jackson module for serializing kotlinx.datetime types.
 *
 * Serializes kotlinx.datetime.Instant as ISO-8601 string: "2025-10-14T13:37:02.652106Z"
 */
@Configuration
class KotlinxDateTimeConfiguration {

    @Bean
    fun kotlinxLocalDateTimeModule(): SimpleModule {
        val module = SimpleModule("KotlinxDateTimeModule")
        module.addSerializer(LocalDateTime::class.java, InstantSerializer())
        module.addDeserializer(LocalDateTime::class.java, InstantDeserializer())
        return module
    }
}

/**
 * Serializes kotlinx.datetime.Instant to ISO-8601 string format.
 * Example: "2025-10-14T13:37:02.652106Z"
 */
class InstantSerializer : JsonSerializer<LocalDateTime>() {
    override fun serialize(value: LocalDateTime?, gen: JsonGenerator, serializers: SerializerProvider) {
        if (value != null) {
            gen.writeString(value.toString())
        } else {
            gen.writeNull()
        }
    }
}

/**
 * Deserializes ISO-8601 string to kotlinx.datetime.Instant.
 * Example: "2025-10-14T13:37:02.652106Z" -> Instant
 */
class InstantDeserializer : JsonDeserializer<LocalDateTime>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime {
        return LocalDateTime.parse(p.text)
    }
}
