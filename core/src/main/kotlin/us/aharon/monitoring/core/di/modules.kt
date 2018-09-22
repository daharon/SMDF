/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.module.module

import us.aharon.monitoring.core.db.TableNameResolver


internal val modules = module {
    single<KLogger> { (name: String) -> KotlinLogging.logger(name) }
    single<AmazonSNS> { AmazonSNSClientBuilder.standard().build() }
    single<ObjectMapper> { ObjectMapper().registerKotlinModule() }
    single<DynamoDBMapper> {
        val client = AmazonDynamoDBClientBuilder.standard().build()
        val config = DynamoDBMapperConfig.builder()
                .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.UPDATE)
                // Set our own table name resolution.
                .withTableNameResolver(TableNameResolver())
                .build()
        DynamoDBMapper(client, config)
    }
    single<AmazonSQS> { AmazonSQSClientBuilder.standard().build() }
}


