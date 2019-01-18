/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.*

import java.time.ZonedDateTime


/**
 * DynamoDB record for client registration.
 * Keep track of the client queues.
 *
 * The table name is dynamically defined in [TableNameResolver].
 */
@DynamoDBTable(tableName = "DYNAMICALLY_DEFINED")
internal data class ClientRecord(

        @DynamoDBHashKey
        @DynamoDBNamed("pk")
        var name: String? = null,

        @DynamoDBAttribute
        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.L)
        var tags: List<String>? = null,

        @DynamoDBAttribute
        var queueArn: String? = null,

        @DynamoDBAttribute
        var queueUrl: String? = null,

        @DynamoDBAttribute
        var subscriptionArn: String? = null
) {
    @DynamoDBRangeKey
    @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
    @DynamoDBAutoGenerated(generator = DynamoDBTimestampGenerator::class)
    @DynamoDBTypeConverted(converter = ZonedDateTimeConverter::class)
    @DynamoDBNamed("sk")
    var createdAt: ZonedDateTime? = null

    @DynamoDBAttribute
    @DynamoDBNamed("data")
    var data: String? = DATA_FIELD

    @DynamoDBAttribute
    @DynamoDBConvertedBool(value = DynamoDBConvertedBool.Format.true_false)
    var active: Boolean? = true

    companion object {
        const val DATA_FIELD = "CLIENT"
    }
}

