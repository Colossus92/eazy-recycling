package nl.eazysoftware.eazyrecyclingservice.config

import ch.qos.logback.classic.spi.ILoggingEvent
import com.logtail.logback.LogtailAppender

/**
 * A custom Logtail appender that safely handles log event arguments.
 *
 * The default LogtailAppender tries to serialize event.getArgumentArray() which can contain
 * complex objects like PgConnection (with circular references) or JobRunr's BackgroundJobServer
 * (with java.time.Duration fields). This causes serialization errors:
 *
 * 1. Document nesting depth exceeded (1001 > 1000) - from circular references in PgConnection
 * 2. Java 8 date/time type `java.time.Duration` not supported - from JobRunr objects
 *
 * This appender overrides buildPostData to exclude the raw args array, preventing these errors.
 * The formatted message (which already contains the interpolated arguments) is still included.
 */
class SafeLogtailAppender : LogtailAppender() {

    override fun buildPostData(event: ILoggingEvent): MutableMap<String, Any> {
        val logLine = super.buildPostData(event)
        // Remove the raw args array to prevent serialization issues with complex objects
        // The formatted message already contains the interpolated argument values
        logLine.remove("args")
        return logLine
    }
}
