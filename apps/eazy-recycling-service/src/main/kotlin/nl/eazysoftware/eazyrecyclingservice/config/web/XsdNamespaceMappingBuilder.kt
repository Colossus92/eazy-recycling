package nl.eazysoftware.eazyrecyclingservice.config.web

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory

class XsdNamespaceMappingBuilder {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(XsdNamespaceMappingBuilder::class.java)

        /**
         * Recursively scans the provided schemaDir for XSD files and builds a mapping of
         * element local names to their target namespace.
         */
        fun buildMapping(): Map<String, String> {
            val mapping = mutableMapOf<String, String>()

            val allowedPaths = listOf(
                "UBL-Waybill-2.1.xsd",
                "common/UBL-CommonAggregateComponents-2.1.xsd",
                "common/UBL-CommonBasicComponents-2.1.xsd",
                "common/UBL-CommonExtensionComponents-2.1.xsd",
                "EBA-Extensions.xsd"
            )

            val resolver = PathMatchingResourcePatternResolver()
            allowedPaths.forEach { relativePath ->
                val resourcePath = "classpath:schema/$relativePath"
                val resource: Resource = resolver.getResource(resourcePath)
                if (resource.exists()) {
                    processXsdResource(resource, mapping)
                } else {
                    logger.warn("Resource not found: $resourcePath")
                }
            }
            return mapping
        }

        private fun processXsdResource(resource: Resource, mapping: MutableMap<String, String>) {
            try {
                val dbf = DocumentBuilderFactory.newInstance()
                dbf.isNamespaceAware = true
                val builder = dbf.newDocumentBuilder()
                resource.inputStream.use { inputStream ->
                    val doc: Document = builder.parse(inputStream)
                    val schemaElement = doc.documentElement as Element
                    val targetNamespace = schemaElement.getAttribute("targetNamespace")
                    if (targetNamespace.isEmpty()) {
                        return
                    }

                    val elements: NodeList = schemaElement.getElementsByTagNameNS("http://www.w3.org/2001/XMLSchema", "element")
                    for (i in 0 until elements.length) {
                        val el = elements.item(i) as Element
                        val name = el.getAttribute("name")
                        if (name.isNotEmpty()) {
                            if (mapping.containsKey(name) && mapping[name] != targetNamespace) {
                                /**
                                 * This warning is thrown for:
                                 *  Condition
                                 *  Duty
                                 *  Location
                                 *  Password
                                 *
                                 *  For these fields the CommonBasicComponents should NOT be used, hence the last place in the list.
                                 *
                                 */
                                logger.warn(
                                    "Element '$name' is defined with multiple target namespaces: " +
                                            "${mapping[name]} and $targetNamespace in file: ${resource.filename}"
                                )
                            } else {
                                mapping[name] = targetNamespace
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                logger.error("Error processing file: ${resource.filename}", ex)
            }
        }
    }
}