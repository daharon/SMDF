/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.module.module


internal val modules = module {
    single<KLogger> { (name: String) -> KotlinLogging.logger(name) }
    single<AmazonSNS> {
        AmazonSNSClientBuilder.standard()
                .withEndpointConfiguration(
                        AwsClientBuilder.EndpointConfiguration("http://localhost:4575", "us-east-1"))
                .build()
    }
}
