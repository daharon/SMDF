/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di

import com.amazonaws.ClientConfiguration
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.module.module

import us.aharon.monitoring.core.db.ClientRecord
import us.aharon.monitoring.core.util.Env


private const val LOCALSTACK_SNS_PORT = 4575
private const val LOCALSTACK_SQS_PORT = 4576
private const val LOCALSTACK_DYNAMODB_PORT = 4569

/**
 * Test modules.
 *
 * - Override the AWS services with clients that point to LocalStack.
 *     - https://github.com/localstack/localstack
 */
internal val modules = module {
    single<Env> { Env(TEST_ENVIRONMENT_VARIABLES) }
    single<KLogger> { (name: String) -> KotlinLogging.logger(name) }
    single<AmazonSNS> {
        AmazonSNSClientBuilder.standard()
                .withClientConfiguration(
                        ClientConfiguration()
                                .withConnectionTimeout(10_000)
                                .withRequestTimeout(10_000))
                .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration("http://localhost:$LOCALSTACK_SNS_PORT", "us-east-1"))
                .build()
    }
    single<AmazonSQS> {
        AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration("http://localhost:$LOCALSTACK_SQS_PORT", "us-east-1"))
                .build()
    }
    single<DynamoDBMapper> {
        val client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration("http://localhost:$LOCALSTACK_DYNAMODB_PORT", "us-east-1"))
                .build()
        val config = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .withTableNameResolver { clazz: Class<*>, _: DynamoDBMapperConfig? ->
                    when (clazz) {
                        ClientRecord::class.java -> "CLIENTS_TABLE"
                        else -> throw DynamoDBMappingException("Class must be defined in ${this::class.qualifiedName}")
                    }
                }
                .build()
        DynamoDBMapper(client, config)
    }
}
