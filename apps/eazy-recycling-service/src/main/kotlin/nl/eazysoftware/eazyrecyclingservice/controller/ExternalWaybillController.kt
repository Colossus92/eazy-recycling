package nl.eazysoftware.eazyrecyclingservice.controller

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import nl.eazysoftware.eazyrecyclingservice.config.web.DynamicNamespaceFilter
import nl.eazysoftware.eazyrecyclingservice.domain.mapper.WaybillMapper
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import oasis.names.specification.ubl.schema.xsd.waybill_2.Waybill
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.xml.sax.InputSource
import java.io.StringWriter
import javax.xml.parsers.SAXParserFactory

@RestController
@RequestMapping("/")
class ExternalWaybillController(
    private val mapper: WaybillMapper,
    private val transportService: TransportService,
    private val dynamicNamespaceFilter: DynamicNamespaceFilter,
) {
    private val logger: Logger = LoggerFactory.getLogger(ExternalWaybillController::class.java)
    private val jaxbContext: JAXBContext = JAXBContext.newInstance(Waybill::class.java)
    private val saxParserFactory: SAXParserFactory = SAXParserFactory.newInstance()

    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    fun confirmRoot() {
        logger.info("Received GET request on root path")
    }


    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/ws")
    fun receiveWaybill(@RequestBody body: String) {
        logger.info("Received Waybill XML:\n$body")
        val xmlWithNamespace = if (!body.contains("xmlns=")) {
            body.replace("<Waybill", """<Waybill xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="urn:oasis:names:specification:ubl:schema:xsd:Waybill-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:ext="urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2" xmlns:eba="http://ns.tln.nl/eba/schemas/1-0/" xsi:schemaLocation="urn:oasis:names:specification:ubl:schema:xsd:Waybill-2 http://docs.oasis-open.org/ubl/prd1-UBL-2.1/xsd/maindoc/UBL-Waybill-2.1.xsd http://ns.tln.nl/eba/schemas/1-0/ ..\..\schemas\1-0-1\EBA-Extensions.xsd"""")
        } else body

        val waybill = unmarshal(xmlWithNamespace)

        val dto = mapper.toDto(waybill)
        transportService.save(dto)
    }

    private fun unmarshal(xmlWithNamespace: String): Waybill {
        val saxParser = saxParserFactory.newSAXParser()
        val xmlReader = saxParser.xmlReader
        val unmarshaller = jaxbContext.createUnmarshaller()

        dynamicNamespaceFilter.parent = xmlReader
        dynamicNamespaceFilter.contentHandler = unmarshaller.unmarshallerHandler

        dynamicNamespaceFilter.parse(InputSource(xmlWithNamespace.reader()))

        return unmarshaller.unmarshallerHandler.result as Waybill
    }
}