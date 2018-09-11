/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.http

import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogger
import org.koin.core.parameter.parametersOf
import org.koin.log.Logger.SLF4JLogger
import org.koin.standalone.KoinComponent
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.inject

import us.aharon.monitoring.core.di.modules


/**
 * Abstract base class for HTTP (API Gateway) handlers.
 *
 * - Load dependency injection modules.
 */
abstract class BaseRequestHandler : KoinComponent, RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    protected val log: KLogger by inject { parametersOf(this::class.java.simpleName) }
    protected val json: ObjectMapper by inject()

    init {
        startKoin(listOf(modules), logger = SLF4JLogger())
    }
}
