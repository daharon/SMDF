/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.extensions

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.db.CheckResultRecord


class CheckResultTableExtension : KoinTest,
        BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private val db: DynamoDBMapper by inject()
    private val client: AmazonDynamoDB by inject()

    override fun beforeTestExecution(context: ExtensionContext) {
        val createTableRequest = db.generateCreateTableRequest(CheckResultRecord::class.java)
                .withProvisionedThroughput(ProvisionedThroughput(1, 1))
        client.createTable(createTableRequest)
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val deleteTableRequest = db.generateDeleteTableRequest(CheckResultRecord::class.java)
        client.deleteTable(deleteTableRequest)
    }
}
