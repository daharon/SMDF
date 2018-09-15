/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel


/**
 * DynamoDB table for client registration.
 * Keep track of the client queues.
 */
@DynamoDBTable(tableName = CLIENTS_DB_TABLE_NAME)
internal data class ClientRecord(

        @DynamoDBHashKey
        var name: String? = null,

        @DynamoDBAttribute
        @DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.L)
        var tags: List<String>? = null,

        @DynamoDBAttribute
        var queueArn: String? = null,

        @DynamoDBAttribute
        var subscriptionArn: String? = null
)
