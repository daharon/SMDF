/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.ClientRecord


/**
 * Process the messages provided by the DynamoDB stream.
 *
 * - Process check results.
 * - Process client changes.
 */
internal class DatabaseStreamProcessor : KoinComponent {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    private val _clientCleanup by lazy { ClientCleanup() }
    private val _checkResultProcessor by lazy { CheckResultProcessor() }


    fun run(event: DynamodbEvent, checks: List<CheckGroup>) {
        event.records.forEach {
            // Prefer the oldImage because the DynamoDB event may have been a deletion.
            val image = it.dynamodb.oldImage ?: it.dynamodb.newImage
            // Determine if this is a client or result record.
            when (pkPrefix(image)) {
                ClientRecord.CLIENT_PK_PREFIX -> handleClientRecord(it)
                CheckResultRecord.RESULT_PK_PREFIX -> handleCheckResultRecord(it, checks)
                else -> log.error("Unable to determine record type.")
            }
        }
    }

    /**
     * Extract the prefix string from the primary (hash) key of the
     * image provided by DynamoDB.
     */
    private fun pkPrefix(image: Map<String, AttributeValue>): String =
            image.getOrDefault("pk", defaultValue = AttributeValue("")).s
                    .takeWhile { it != '#' }

    private fun handleCheckResultRecord(record: DynamodbEvent.DynamodbStreamRecord, checks: List<CheckGroup>) {
        _checkResultProcessor.run(record, checks)
    }

    private fun handleClientRecord(record: DynamodbEvent.DynamodbStreamRecord) {
        // TODO: Update this for more than just client deletion.
        _clientCleanup.run(record)
    }

}
