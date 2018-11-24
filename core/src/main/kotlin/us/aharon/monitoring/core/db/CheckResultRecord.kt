/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.*

import us.aharon.monitoring.core.util.toMd5HexString

import java.time.ZonedDateTime


/**
 * DynamoDB table for check results.
 *
 * The table name is dynamically defined in [TableNameResolver].
 */
@DynamoDBTable(tableName = "DYNAMICALLY_DEFINED")
data class CheckResultRecord(
        @DynamoDBRangeKey
        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
        @DynamoDBTypeConverted(converter = ZonedDateTimeConverter::class)
        var timestamp: ZonedDateTime? = null,

        @DynamoDBAttribute
        var group: String? = null,
        @DynamoDBAttribute
        var name: String? = null,
        @DynamoDBAttribute
        var source: String? = null,
        @DynamoDBAttribute
        @DynamoDBTypeConvertedEnum
        var status: CheckResultStatus? = null,
        @DynamoDBAttribute
        var output: String? = null
) {
    @DynamoDBHashKey
    var id: String? = null
            get() = generateId(this.group!!, this.name!!, this.source!!)


    companion object {
        fun generateId(group: String, name: String, source: String): String =
                // Convert to an MD5 hash because the existing ID could end up being too long.
                "${group}_${name}_${source}".toMd5HexString()
    }
}

