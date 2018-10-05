/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable


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
        var timestamp: String? = null
)

