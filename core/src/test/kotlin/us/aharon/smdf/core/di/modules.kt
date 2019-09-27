/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.di

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult
import com.amazonaws.services.simplesystemsmanagement.model.Parameter
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.SubscribeResult
import com.amazonaws.services.sns.model.UnsubscribeRequest
import com.amazonaws.services.sns.model.UnsubscribeResult
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.TagQueueResult
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.mockk.every
import io.mockk.spyk
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.module

import us.aharon.smdf.core.common.FAKE_SNS_SUBSCRIPTION_ARN
import us.aharon.smdf.core.db.*
import us.aharon.smdf.core.extensions.DYNAMODB_TEST_TABLE_NAME
import us.aharon.smdf.core.util.Env


private const val LOCALSTACK_SNS_PORT = 4575
private const val LOCALSTACK_DYNAMODB_PORT = 4569


/**
 * Test modules.
 *
 * - Override the AWS services with clients that point to LocalStack.
 *     - https://github.com/localstack/localstack
 */
internal val modules = module {
    single<AWSCredentialsProvider> {
        val credentials = BasicAWSCredentials("abc123", "abc123")
        AWSStaticCredentialsProvider(credentials)
    }
    single<Env> { Env(TEST_ENVIRONMENT_VARIABLES) }
    factory<KLogger> { (name: String) -> KotlinLogging.logger(name) }
    single<ObjectMapper> {
        ObjectMapper()
                .registerModule(JavaTimeModule())
                .registerKotlinModule()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
    single<AmazonSNS> { mockedAmazonSNS(get()) }
    single<AmazonSQS> { mockedAmazonSQS() }
    single<DynamoDBMapper> {
        val config = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                .withTableNameResolver { clazz: Class<*>, _: DynamoDBMapperConfig? ->
                    when (clazz) {
                        ClientRecord::class.java -> DYNAMODB_TEST_TABLE_NAME
                        ClientHistoryRecord::class.java -> DYNAMODB_TEST_TABLE_NAME
                        CheckResultRecord::class.java -> DYNAMODB_TEST_TABLE_NAME
                        NotificationRecord::class.java -> DYNAMODB_TEST_TABLE_NAME
                        else -> throw DynamoDBMappingException("Class must be defined in ${this::class.qualifiedName}")
                    }
                }
                .build()
        DynamoDBMapper(get(), config)
    }
    single<AmazonDynamoDB> {
        AmazonDynamoDBClientBuilder.standard()
                .withCredentials(get())
                .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration("http://localhost:$LOCALSTACK_DYNAMODB_PORT", "us-east-1"))
                .build()
    }
    single<AWSSimpleSystemsManagement> { mockedAWSSimpleSystemsManagement(get()) }
    single<Dao> { Dao() }
}

/**
 * Mock the [AmazonSQS] class because some functionality is not provided by LocalStack.
 */
private fun mockedAmazonSQS(): AmazonSQS {
    val sqs = spyk(cloud.localstack.TestUtils.getClientSQS())
    // Calling AmazonSQS::tagQueue on Localstack causes error.
    every { sqs.tagQueue(any(), any()) } returns TagQueueResult()
    return sqs
}

/**
 * Mock the [AmazonSNS] class because some functionality is not provided by LocalStack.
 */
private fun mockedAmazonSNS(credentials: AWSCredentialsProvider): AmazonSNS {
    val sns: AmazonSNS = spyk(
            AmazonSNSClientBuilder.standard()
                    .withCredentials(credentials)
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

/**
 * Mock the [AWSSimpleSystemsManagement] class because it doesn't work locally.
 */
private fun mockedAWSSimpleSystemsManagement(credentials: AWSCredentialsProvider): AWSSimpleSystemsManagement {
    val ssm: AWSSimpleSystemsManagement = spyk(
            AWSSimpleSystemsManagementClientBuilder.standard()
                    .withCredentials(credentials)
                    .withRegion(Regions.US_EAST_1)
                    .build())
    every { ssm.getParameter(any()) } returns GetParameterResult().withParameter(Parameter().withValue(""))
    return ssm
}
