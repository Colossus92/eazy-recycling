package nl.eazysoftware.eazyrecyclingservice.config.web

import org.springframework.stereotype.Component
import org.xml.sax.Attributes
import org.xml.sax.helpers.XMLFilterImpl


@Component
class DynamicNamespaceFilter : XMLFilterImpl() {

    private val xsdNamespaceMapping = XsdNamespaceMappingBuilder.buildMapping()

    override fun startElement(uri: String?, localName: String, qName: String, atts: Attributes) {
        val effectiveUri = if (uri.isNullOrBlank()) xsdNamespaceMapping[qName] ?: "" else uri
        super.startElement(effectiveUri, localName, qName, atts)
    }

    override fun endElement(uri: String?, localName: String, qName: String) {
        val effectiveUri = if (uri.isNullOrBlank()) xsdNamespaceMapping[qName] ?: "" else uri
        super.endElement(effectiveUri, localName, qName)
    }
}