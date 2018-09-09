/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.di

import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.module.module
import org.koin.standalone.KoinComponent


internal val modules = module {
    single<KLogger> { (name: String) -> KotlinLogging.logger(name) }
    single<AmazonSNS> { AmazonSNSClientBuilder.standard().build() }
}

internal val DI: KoinComponent by lazy { object : KoinComponent { } }
