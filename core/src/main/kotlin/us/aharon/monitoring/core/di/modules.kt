/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.module.module


internal val modules = module {
    single<KLogger> { (name: String) -> KotlinLogging.logger(name) }
    single<AmazonSNS> { AmazonSNSClientBuilder.standard().build() }
    single<ObjectMapper> { ObjectMapper().registerKotlinModule() }
}
