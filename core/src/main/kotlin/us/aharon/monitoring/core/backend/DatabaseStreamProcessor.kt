/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.backend

import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.OperationType
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.checks.CheckGroup
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.ClientHistoryRecord
import us.aharon.monitoring.core.db.ClientRecord
import us.aharon.monitoring.core.db.NotificationRecord


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
            val image = when (OperationType.fromValue(it.eventName)) {
                OperationType.INSERT -> it.dynamodb.newImage
                OperationType.MODIFY -> it.dynamodb.newImage
                OperationType.REMOVE -> it.dynamodb.oldImage
                null -> throw Exception("Unknown event name provided:  ${it.eventName}")
            }
            // Determine which kind of record this is.
            val dataField = image.getOrDefault("data", defaultValue = AttributeValue("")).s
            when (dataField) {
                ClientRecord.DATA_FIELD -> handleClientRecord(it)
                CheckResultRecord.DATA_FIELD -> handleCheckResultRecord(it, checks)
                ClientHistoryRecord.DATA_FIELD -> log.debug { "$dataField record:  Do nothing" }
                NotificationRecord.DATA_FIELD -> log.debug { "$dataField record:  Do nothing" }
                else -> log.error("Unable to determine record type.")
            }
        }
    }

    private fun handleCheckResultRecord(record: DynamodbEvent.DynamodbStreamRecord, checks: List<CheckGroup>) {
        _checkResultProcessor.run(record, checks)
    }

    private fun handleClientRecord(record: DynamodbEvent.DynamodbStreamRecord) {
        // TODO: Update this for more than just client deletion.
        _clientCleanup.run(record)
    }

}
