package nl.eazysoftware.eazyrecyclingservice.domain.mapper

import jakarta.xml.bind.annotation.adapters.XmlAdapter
import java.time.LocalTime

class LocalTimeXmlAdapter: XmlAdapter<String, LocalTime>() {
    override fun unmarshal(string: String): LocalTime {
        return LocalTime.parse(string)
    }

    override fun marshal(localTime: LocalTime?): String {
        return localTime.toString()
    }
}