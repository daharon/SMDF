/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.amazonaws.services.securitytoken.AWSSecurityTokenService
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.backend.messages.ServerlessCheckMessage
import us.aharon.monitoring.core.checks.*
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.util.Env

import java.time.ZonedDateTime


/**
 * Run the serverless check for the given message.
 *
 * - Instantiate the given serverless check class.
 * - Run the check.
 */
internal class ServerlessCheckProcessor : KoinComponent {

    private val env: Env by inject()
    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val json: ObjectMapper by inject()
    private val sqs: AmazonSQS by inject()
    private val sts: AWSSecurityTokenService by inject()
    private val ssm: AWSSimpleSystemsManagement by inject()

    private val CHECK_RESULT_QUEUE by lazy { env.get("CHECK_RESULTS_QUEUE") }


    fun run(event: SQSEvent, checks: List<CheckGroup>, context: Context) = event.records.forEach { message ->
        val checkMsg = json.readValue<ServerlessCheckMessage>(message.body)
        val check = checks.getCheck(checkMsg.group, checkMsg.name) as ServerlessCheck
        log.info("Running serverless check:  ${checkMsg.executor}")
        // Create an instance of the executor given its fully qualified name.
        val serverlessExecutor = Class.forName(checkMsg.executor).newInstance() as ServerlessExecutor

        // AssumeRole based on the permissions defined in the ServerlessExecutor instance.
        val originalRole = sts.getCallerIdentity(GetCallerIdentityRequest())
        log.info { "Existing IAM role:  $originalRole" }

        // Get the role ARN from the parameter store.
        val paramRequest = GetParameterRequest()
                .withName("${serverlessExecutorParameterPath(env.get("ENVIRONMENT"))}/${checkMsg.executor}")
        val serverlessExecutorRoleArn = ssm.getParameter(paramRequest).parameter.value
        log.info { "Serverless executor role ARN:  $serverlessExecutorRoleArn" }

        // Assume serverless executor role.
        val assumeRoleRequest = AssumeRoleRequest()
                .withRoleArn(serverlessExecutorRoleArn)
                .withRoleSessionName("${this::class.java.simpleName}-${env.get("ENVIRONMENT")}")
        val assumeRoleResult = sts.assumeRole(assumeRoleRequest)
        log.info { "Assumed IAM role:  $assumeRoleResult" }

        // Execute the check.
        val executedAt = ZonedDateTime.now()
        val result: Result = try {
            serverlessExecutor.execute(check, context)
        } catch (e: Exception) {
            val errorMsg = "Error running serverless check:  ${checkMsg.executor}\n${e.message}"
            log.error(errorMsg)
            Critical(output = errorMsg)
        } finally {
            // TODO:  Do we need to re-assume the original IAM role?
        }

        // Send the result to the result queue.
        val resultMessage = json.writeValueAsString(CheckResultRecord(
                completedAt= ZonedDateTime.now(),
                scheduledAt = checkMsg.scheduledAt,
                executedAt = executedAt,
                group = checkMsg.group,
                name = checkMsg.name,
                source = checkMsg.executor,
                status = result.status,
                output = result.output
        ))
        val req = SendMessageRequest(CHECK_RESULT_QUEUE, resultMessage)
        val sendMessageResult = sqs.sendMessage(req)
        log.info { "Sent result message to message queue:  $sendMessageResult" }
    }
}
