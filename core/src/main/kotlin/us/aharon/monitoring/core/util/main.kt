package us.aharon.monitoring.core.util

import freemarker.template.Configuration as TemplateConfiguration

import kotlin.reflect.KClass
import java.io.StringWriter


/**
 * CloudFormation template filename from the package's resources directory.
 */
private const val CLOUDFORMATION_TEMPLATE = "cloudformation.yaml"


/**
 * Render the CloudFormation template.
 *
 * @param clazz The resource loader class.
 * @param config Configuration variables.
 */
internal fun renderCloudFormationTemplate(clazz: KClass<out Any>, options: CLIArgs): String {
    val templateConfig = TemplateConfiguration(TemplateConfiguration.VERSION_2_3_28).apply {
        setClassForTemplateLoading(clazz.java, "/")
        defaultEncoding = "UTF-8"
    }
    val templateCfn = templateConfig.getTemplate(CLOUDFORMATION_TEMPLATE)
    val templateData = mapOf<String, Any>(
            // TODO:  Figure out how to get the canonical name of the class that extends [Application].
            "clientRegistrationHandler" to "${clazz.java.canonicalName}.clientRegistrationHandler"
    )
    val renderedTemplate = StringWriter()
    templateCfn.process(templateData, renderedTemplate)
    return renderedTemplate.toString()
}
