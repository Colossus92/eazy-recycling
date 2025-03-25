package nl.eazysoftware.eazyrecyclingservice.controller

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import nl.eazysoftware.eazyrecyclingservice.domain.mapper.WaybillMapper
import oasis.names.specification.ubl.schema.xsd.waybill_2.Waybill
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ws.server.endpoint.annotation.Endpoint
import org.springframework.ws.server.endpoint.annotation.PayloadRoot
import org.springframework.ws.server.endpoint.annotation.RequestPayload
import org.springframework.ws.server.endpoint.annotation.ResponsePayload
import java.io.StringWriter

@Endpoint
class WaybillEndpoint(
    private val mapper: WaybillMapper,
    private val transportService: TransportService,
) {

    private val logger: Logger = LoggerFactory.getLogger(WaybillEndpoint::class.java)

    companion object {
        const val NAMESPACE_URI = "urn:oasis:names:specification:ubl:schema:xsd:Waybill-2"
    }

    @Suppress("unused")
    @ResponsePayload
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "Waybill")
    fun receiveWaybill(@RequestPayload waybill: Waybill): Waybill {
        val xml = marshalToXml(waybill)
        logger.info("Received SOAP Waybill XML:\n$xml")

        val dto = mapper.toDto(waybill)
        transportService.save(dto)

        return waybill
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