/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBNamed
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable


/**
 * DynamoDB record for notification events.
 * Record notifications that have been sent and the check results that triggered them.
 *
 * The table name is dynamically defined in [TableNameResolver].
 */
@DynamoDBTable(tableName = "DYNAMICALLY_DEFINED")
internal data class NotificationRecord(
        @DynamoDBHashKey
        @DynamoDBNamed("pk")
        var handler: String? = null,

        @DynamoDBAttribute
        @DynamoDBNamed("sk")
        var timestamp: String? = null,

        @DynamoDBAttribute
        @DynamoDBNamed("result_id")
        var resultId: String? = null,

        @DynamoDBAttribute
        var description: String? = null

) {
    @DynamoDBAttribute
    @DynamoDBNamed("data")
    var data: String? = DATA_FIELD

    companion object {
        const val DATA_FIELD = "NOTIFICATION"
    }
}
