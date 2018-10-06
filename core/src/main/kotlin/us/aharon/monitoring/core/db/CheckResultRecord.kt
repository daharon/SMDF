/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.*

import java.time.ZonedDateTime


/**
 * DynamoDB table for check results.
 *
 * The table name is dynamically defined in [TableNameResolver].
 */
@DynamoDBTable(tableName = "DYNAMICALLY_DEFINED")
internal data class CheckResultRecord(
        @DynamoDBHashKey
        var name: String? = null,

        @DynamoDBRangeKey
        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
        @DynamoDBTypeConverted(converter = ZonedDateTimeConverter::class)
        var timestamp: ZonedDateTime? = null,

        @DynamoDBAttribute
        @DynamoDBTypeConvertedEnum
        var status: CheckResultStatus? = null,

        @DynamoDBAttribute
        var output: String? = null
)

