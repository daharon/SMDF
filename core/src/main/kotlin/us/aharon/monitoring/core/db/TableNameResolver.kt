/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException


/**
 * Custom DynamoDB table name resolver.
 * Because the table names are created by CloudFormation, we cannot know them at compile time.
 * This class maps [com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable] classes to
 * the DynamoDB table names set in environment variables.
 */
internal class TableNameResolver : DynamoDBMapperConfig.TableNameResolver {

    override fun getTableName(clazz: Class<*>, config: DynamoDBMapperConfig?): String = when (clazz) {
        ClientRecord::class.java -> CLIENTS_DB_TABLE_NAME.orEmpty()
        else -> throw DynamoDBMappingException(
                "Cannot find database name for $clazz. Must be defined in ${this::class.qualifiedName}.")
    }
}
