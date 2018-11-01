/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.db.CheckResultRecord


/**
 * Receive check results from the result queue and write them to the database.
 */
internal class CheckResultReceiver : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val db: DynamoDBMapper by inject()
    private val json: ObjectMapper by inject()


    fun run(event: SQSEvent) {
        val records: List<CheckResultRecord> = event.records.mapNotNull {
            try {
                json.readValue<CheckResultRecord>(it.body)
            } catch (e: JsonMappingException) {
                log.error("Failed to parse message body:  ${it.body}")
                null
            }
        }
        db.batchSave(records)
    }
}
