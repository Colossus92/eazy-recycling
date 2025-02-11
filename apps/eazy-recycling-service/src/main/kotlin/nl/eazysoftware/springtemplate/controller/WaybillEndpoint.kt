package nl.eazysoftware.springtemplate.controller

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import nl.eazysoftware.springtemplate.domain.WaybillTypeDecorator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ws.server.endpoint.annotation.Endpoint
import org.springframework.ws.server.endpoint.annotation.PayloadRoot
import org.springframework.ws.server.endpoint.annotation.RequestPayload
import org.springframework.ws.server.endpoint.annotation.ResponsePayload
import java.io.StringWriter

@Endpoint
class WaybillEndpoint {

    private val logger: Logger = LoggerFactory.getLogger(WaybillEndpoint::class.java)

    companion object {
        const val NAMESPACE_URI = "urn:oasis:names:specification:ubl:schema:xsd:Waybill-2"
    }

    @Suppress("unused")
    @ResponsePayload
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "Waybill")
    fun receiveWaybill(@RequestPayload waybill: WaybillTypeDecorator): WaybillTypeDecorator {
        val xml = marshalToXml(waybill)
        logger.info("Received SOAP Waybill XML:\n$xml")
        return waybill;
    }

    private fun marshalToXml(obj: Any): String {
        val jaxbContext = JAXBContext.newInstance(obj.javaClass)
        val marshaller: Marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        val writer = StringWriter()
        marshaller.marshal(obj, writer)
        return writer.toString()
    }
}