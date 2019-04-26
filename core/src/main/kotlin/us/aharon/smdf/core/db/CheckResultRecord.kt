/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.*

import us.aharon.smdf.core.util.toMd5HexString

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
        @DynamoDBNamed("sk")
        var completedAt: ZonedDateTime? = null,

        @DynamoDBAttribute
        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
        @DynamoDBTypeConverted(converter = ZonedDateTimeConverter::class)
        var scheduledAt: ZonedDateTime? = null,

        @DynamoDBAttribute
        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
        @DynamoDBTypeConverted(converter = ZonedDateTimeConverter::class)
        var executedAt: ZonedDateTime? = null,

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
    @DynamoDBNamed("pk")
    var resultId: String? = null
            get() = generateResultId(this.group!!, this.name!!, this.source!!)

    @DynamoDBNamed("data")
    var data: String? = DATA_FIELD


    companion object {
        internal const val DATA_FIELD: String = "CHECK_RESULT"

        fun generateResultId(group: String, name: String, source: String): String =
                // Convert to an MD5 hash because the existing ID could end up being too long.
                "${group}_${name}_${source}".toMd5HexString()
    }
}
