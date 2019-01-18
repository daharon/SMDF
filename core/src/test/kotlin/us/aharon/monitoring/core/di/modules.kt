/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di

import com.amazonaws.ClientConfiguration
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.SubscribeResult
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.amazonaws.services.sns.model.UnsubscribeResult
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.TagQueueResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.spyk
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.module.module

import us.aharon.monitoring.core.common.FAKE_SNS_SUBSCRIPTION_ARN
import us.aharon.monitoring.core.db.CheckResultRecord
import us.aharon.monitoring.core.db.ClientRecord
import us.aharon.monitoring.core.db.Dao
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
    single<ObjectMapper> {
        ObjectMapper()
                .registerModule(JavaTimeModule())
                .registerKotlinModule()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    single<AmazonSNS> { mockedAmazonSNS() }
    single<AmazonSQS> { mockedAmazonSQS() }
    single<DynamoDBMapper> {
        val client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration("http://localhost:$LOCALSTACK_DYNAMODB_PORT", "us-east-1"))
                .build()
        val config = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .withTableNameResolver { clazz: Class<*>, _: DynamoDBMapperConfig? ->
                    when (clazz) {
                        ClientRecord::class.java -> "TEST_TABLE"
                        CheckResultRecord::class.java -> "CHECK_RESULTS_TABLE"
                        else -> throw DynamoDBMappingException("Class must be defined in ${this::class.qualifiedName}")
                    }
                }
                .build()
        DynamoDBMapper(client, config)
    }
    single<AmazonDynamoDB> {
        AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration("http://localhost:$LOCALSTACK_DYNAMODB_PORT", "us-east-1"))
                .build()
    }
    single<Dao> { Dao() }
}

/**
 * Mock the [AmazonSQS] class because some functionality is not provided by LocalStack.
 */
private fun mockedAmazonSQS(): AmazonSQS {
    val sqs = spyk(
            AmazonSQSClientBuilder.standard()
                    .withEndpointConfiguration(
                            AwsClientBuilder.EndpointConfiguration("http://localhost:$LOCALSTACK_SQS_PORT", "us-east-1"))
                    .build())
    // Calling AmazonSQS::tagQueue on Localstack causes error.
    every { sqs.tagQueue(any(), any()) } returns TagQueueResult()
    return sqs
}

/**
 * Mock the [AmazonSNS] class because some functionality is not provided by LocalStack.
 */
private fun mockedAmazonSNS(): AmazonSNS {
    val sns: AmazonSNS = spyk(
            AmazonSNSClientBuilder.standard()
                    .withClientConfiguration(
                            ClientConfiguration()
                                    .withConnectionTimeout(10_000)
                                    .withRequestTimeout(10_000))
                    .withEndpointConfiguration(
                            AwsClientBuilder.EndpointConfiguration("http://localhost:$LOCALSTACK_SNS_PORT", "us-east-1"))
                    .build())
    // Calling AmazonSNS::subscribe on Localstack causes error.
    every { sns.subscribe(any()) } returns SubscribeResult().withSubscriptionArn(FAKE_SNS_SUBSCRIPTION_ARN)
    every { sns.unsubscribe(UnsubscribeRequest(FAKE_SNS_SUBSCRIPTION_ARN)) } returns UnsubscribeResult()
    return sns
}
