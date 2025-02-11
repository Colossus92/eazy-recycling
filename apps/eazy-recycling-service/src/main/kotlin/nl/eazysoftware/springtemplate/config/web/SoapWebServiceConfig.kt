package nl.eazysoftware.springtemplate.config.web

import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.web.server.adapter.WebHttpHandlerBuilder.applicationContext
import org.springframework.ws.config.annotation.EnableWs
import org.springframework.ws.config.annotation.WsConfigurerAdapter
import org.springframework.ws.transport.http.MessageDispatcherServlet
import org.springframework.xml.xsd.SimpleXsdSchema
import org.springframework.xml.xsd.XsdSchema


@EnableWs
@Configuration
class SoapWebServiceConfig : WsConfigurerAdapter() {

    @Bean
    fun messageDispatcherServlet(applicationContext: ApplicationContext): ServletRegistrationBean<MessageDispatcherServlet> {
        val servlet = MessageDispatcherServlet()
        servlet.setApplicationContext(applicationContext)
        servlet.isTransformWsdlLocations = true
        return ServletRegistrationBean(servlet, "/ws/*")
    }

    @Bean(name = ["Waybill"])
    fun defaultWsdl11Definition(): org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition {
        val wsdl = org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition()
        wsdl.setPortTypeName("EBAPortType")
        wsdl.setLocationUri("/ws")
        wsdl.setTargetNamespace("urn:oasis:names:specification:ubl:schema:xsd:Waybill-2")
        wsdl.setSchema(waybillSchema())
        return wsdl
    }

    @Bean
    fun waybillSchema(): XsdSchema {
        return SimpleXsdSchema(ClassPathResource("schema/UBL-Waybill-2.1.xsd"))
    }
}