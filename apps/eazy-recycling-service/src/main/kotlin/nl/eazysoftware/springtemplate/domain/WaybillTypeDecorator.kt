package nl.eazysoftware.springtemplate.domain
import jakarta.xml.bind.annotation.XmlRootElement
import oasis.names.specification.ubl.schema.xsd.waybill_2.WaybillType

/**
 * Decorator class for the generated WaybillType class.
 */
@XmlRootElement(name = "Waybill", namespace = "urn:oasis:names:specification:ubl:schema:xsd:Waybill-2")
class WaybillTypeDecorator: WaybillType() {
}