/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.*

import java.time.ZonedDateTime


/**
 * DynamoDB record for client history.
 * Keep track of changes that have occurred for a client.
 *
 * The table name is dynamically defined in [TableNameResolver].
 */
@DynamoDBTable(tableName = "DYNAMICALLY_DEFINED")
internal data class ClientHistoryRecord(
        @DynamoDBHashKey
        @DynamoDBNamed("pk")
        var name: String? = null,

        @DynamoDBTypeConverted(converter = ZonedDateTimeConverter::class)
        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
        @DynamoDBNamed("sk")
        var timestamp: ZonedDateTime? = null,

        @DynamoDBAttribute
        var description: String? = null
) {
    @DynamoDBAttribute
    @DynamoDBNamed("data")
    var data: String? = DATA_FIELD

    companion object {
        const val DATA_FIELD = "CLIENT_HISTORY"
    }
}
