/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.smdf.core.backend

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject


internal interface AssumeRoleable {

    val ssm: AWSSimpleSystemsManagement

    fun getCredentials(parameterName: String, sessionName: String): AWSCredentialsProvider
}


/**
 * Mixin for a class that needs credentials for an assumed IAM role.
 */
internal class AssumeRole : AssumeRoleable, KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    override val ssm: AWSSimpleSystemsManagement by inject()


    override fun getCredentials(parameterName: String, sessionName: String): AWSCredentialsProvider {
        // Get the role ARN from the parameter store.
        val paramRequest = GetParameterRequest()
                .withName(parameterName)
        val roleArn = ssm.getParameter(paramRequest).parameter.value
        log.debug { "Role ARN:  $roleArn" }

        // Create an IAM credentials provider that assumes the notification handler's role.
        return STSAssumeRoleSessionCredentialsProvider
                .Builder(roleArn, sessionName)
                .build()
    }

}
