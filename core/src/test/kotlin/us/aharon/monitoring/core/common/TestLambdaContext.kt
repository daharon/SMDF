/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.common

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.ClientContext
import com.amazonaws.services.lambda.runtime.Client
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.CognitoIdentity


class TestLambdaContext(private val functionName: String) : Context {
    override fun getAwsRequestId(): String = "FAKE_REQUEST_ID"
    override fun getLogStreamName(): String = "FAKE_LOG_STREAM_NAME"
    override fun getClientContext(): ClientContext = object : ClientContext {
        override fun getCustom(): MutableMap<String, String> = mutableMapOf()
        override fun getEnvironment(): MutableMap<String, String> = mutableMapOf()
        override fun getClient(): Client = object : Client {
            override fun getAppVersionCode(): String = "0.0.1"
            override fun getAppPackageName(): String = "FAKE"
            override fun getAppTitle(): String = "FAKE"
            override fun getInstallationId(): String = "FAKE"
            override fun getAppVersionName(): String = "FAKE"
        }
    }
    override fun getFunctionName(): String = functionName
    override fun getRemainingTimeInMillis(): Int = 9_000_000
    override fun getLogger(): LambdaLogger = object : LambdaLogger {
        override fun log(p0: String?) = println(p0)
        override fun log(p0: ByteArray?) = println(p0)
    }
    override fun getInvokedFunctionArn(): String = FAKE_LAMBDA_FUNCTION_ARN
    override fun getMemoryLimitInMB(): Int = 3008
    override fun getLogGroupName(): String = "FAKE_LOG_GROUP_NAME"
    override fun getFunctionVersion(): String = "0.1.0"
    override fun getIdentity(): CognitoIdentity = object : CognitoIdentity {
        override fun getIdentityPoolId(): String = "FAKE"
        override fun getIdentityId(): String = "FAKE"
    }
}
