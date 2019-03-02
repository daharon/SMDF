/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
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

import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.*


/**
 * Run the serverless check for the given message.
 *
 * - Instantiate the given serverless check class.
 * - Run the check.
 */
internal class ServerlessCheckProcessor :
        KoinComponent,
        AssumeRoleable by AssumeRole() {

    private val env: Env by inject()
    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val json: ObjectMapper by inject()
    private val sqs: AmazonSQS by inject()
    private val threadExecutor = Executors.newSingleThreadExecutor()

    private val CHECK_RESULT_QUEUE by lazy { env.get("CHECK_RESULTS_QUEUE") }


    fun run(event: SQSEvent, checks: List<CheckGroup>, context: Context) = event.records.forEach { message ->
        val checkMsg = json.readValue<ServerlessCheckMessage>(message.body)
        val check = checks.getCheck(checkMsg.group, checkMsg.name) as ServerlessCheck
        log.info("Running serverless check:  ${checkMsg.executor}")
        log.debug("Check:  $check")
        // Create an instance of the executor given its fully qualified name.
        val serverlessExecutor = Class.forName(checkMsg.executor).newInstance() as ServerlessExecutor

        val credentialsProvider = getCredentials(
                "${serverlessExecutorParameterPath(env.get("ENVIRONMENT"))}/${checkMsg.executor}",
                "${serverlessExecutor::class.java.simpleName}-${env.get("ENVIRONMENT")}")

        // Execute the check.
        val executedAt = ZonedDateTime.now()
        val job: Future<Result> = threadExecutor.submit(object : Callable<Result> {
            override fun call(): Result =
                    serverlessExecutor.run(check, context, credentialsProvider)
        })
        val result: Result = try {
            job.get(checkMsg.timeout.toLong(), TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            job.cancel(true)
            val timeRan = Duration.between(executedAt, ZonedDateTime.now()).seconds
            val errorMsg = "Serverless check timed out after $timeRan seconds:  ${checkMsg.executor}\n${e.message}"
            log.error(errorMsg)
            Unknown(output = errorMsg)
        } catch (e: Exception) {
            val errorMsg = "Error running serverless check:  ${checkMsg.executor}\n${e.message}"
            log.error(errorMsg)
            Critical(output = errorMsg)
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
