package nl.eazysoftware.eazyrecyclingservice.domain.mapper

import jakarta.xml.bind.annotation.adapters.XmlAdapter
import java.time.LocalDate

class LocalDateXmlAdapter: XmlAdapter<String, LocalDate>() {
    override fun unmarshal(string: String): LocalDate {
        return LocalDate.parse(string)
    }

    override fun marshal(localDate: LocalDate?): String {
        return localDate.toString()
    }
}