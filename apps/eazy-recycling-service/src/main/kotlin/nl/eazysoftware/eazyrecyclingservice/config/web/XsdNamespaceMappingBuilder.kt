package nl.eazysoftware.eazyrecyclingservice.config.web

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class XsdNamespaceMappingBuilder {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(XsdNamespaceMappingBuilder::class.java)

        /**
         * Recursively scans the provided schemaDir for XSD files and builds a mapping of
         * element local names to their target namespace.
         */
        fun buildMapping(): Map<String, String> {
            val resource = XsdNamespaceMappingBuilder::class.java.classLoader.getResource("schema")
            if (resource == null) {
                logger.error("Schema resource directory 'schema' not found in classpath")
                return emptyMap()
            }
            val schemaDir = File(resource.toURI())
            if (!schemaDir.exists() || !schemaDir.isDirectory) {
                logger.error("Schema directory is missing or invalid: ${schemaDir.absolutePath}")
                return emptyMap()
            }

            val mapping = mutableMapOf<String, String>()

            val allowedPaths = listOf(
                "UBL-Waybill-2.1.xsd",
                "common/UBL-CommonAggregateComponents-2.1.xsd",
                "common/UBL-CommonExtensionComponents-2.1.xsd",
                "EBA-Extensions.xsd",
                "common/UBL-CommonBasicComponents-2.1.xsd"
            )

            allowedPaths.forEach { relativePath ->
                val xsdFile = File(schemaDir, relativePath)
                if (xsdFile.exists() && xsdFile.isFile) {
                    processXsdFile(xsdFile, mapping)
                } else {
                    logger.warn("File not found or is not a file: ${xsdFile.absolutePath}")
                }
            }

            return mapping
        }

        private fun processXsdFile(xsdFile: File, mapping: MutableMap<String, String>) {
            try {
                val dbf = DocumentBuilderFactory.newInstance()
                dbf.isNamespaceAware = true
                val builder = dbf.newDocumentBuilder()
                val doc: Document = builder.parse(xsdFile)

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
                                        "${mapping[name]} and $targetNamespace in file: ${xsdFile.absolutePath}"
                            )
                        } else {
                            mapping[name] = targetNamespace
                        }
                    }
                }
            } catch (ex: Exception) {
                logger.error("Error processing file: ${xsdFile.absolutePath}", ex)
            }
        }
    }
}