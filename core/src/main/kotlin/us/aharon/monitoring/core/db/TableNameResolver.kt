/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

import us.aharon.monitoring.core.util.Env

import javax.naming.NameNotFoundException


/**
 * Custom DynamoDB table name resolver.
 * Because the table names are created by CloudFormation, we cannot know them at compile time.
 * This class maps [com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable] classes to
 * the DynamoDB table names set in environment variables.
 */
internal class TableNameResolver : DynamoDBMapperConfig.TableNameResolver, KoinComponent {

    private val env: Env by inject()


    override fun getTableName(clazz: Class<*>, config: DynamoDBMapperConfig?): String = try {
        when (clazz) {
            ClientRecord::class.java -> env.get("CLIENT_DB_TABLE")
            else -> throw DynamoDBMappingException(errorMessage(clazz))
        }
    } catch (e: NameNotFoundException) {
        throw DynamoDBMappingException(errorMessage(clazz))
    }

    private fun errorMessage(clazz: Class<*>): String =
            "Cannot find database name for $clazz. Must be defined in ${this::class.qualifiedName}."
}
