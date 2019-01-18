/*
 * Copyright (c) 2018 Daniel Aharon
 */

/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.extensions

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.model.*
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.db.ClientRecord
import us.aharon.monitoring.core.db.DATA_ACTIVE_INDEX
import us.aharon.monitoring.core.db.PK_DATA_INDEX


class ClientsTableExtension : KoinTest,
        BeforeTestExecutionCallback, AfterTestExecutionCallback {

    private val db: DynamoDBMapper by inject()
    private val client: AmazonDynamoDB by inject()

    override fun beforeTestExecution(context: ExtensionContext) {
        val createTableRequest = CreateTableRequest()
                .withTableName("TEST_TABLE")
                .withAttributeDefinitions(
                        AttributeDefinition("pk", ScalarAttributeType.S),
                        AttributeDefinition("sk", ScalarAttributeType.S),
                        AttributeDefinition("data", ScalarAttributeType.S),
                        AttributeDefinition("active", ScalarAttributeType.S))
                .withKeySchema(
                        KeySchemaElement("pk", KeyType.HASH),
                        KeySchemaElement("sk", KeyType.RANGE))
                .withLocalSecondaryIndexes(
                        LocalSecondaryIndex()
                                .withIndexName(PK_DATA_INDEX)
                                .withProjection(Projection().withProjectionType(ProjectionType.ALL))
                                .withKeySchema(
                                        KeySchemaElement("pk", KeyType.HASH),
                                        KeySchemaElement("data", KeyType.RANGE)))
                .withGlobalSecondaryIndexes(
                        GlobalSecondaryIndex()
                                .withIndexName(DATA_ACTIVE_INDEX)
                                .withProjection(Projection().withProjectionType(ProjectionType.ALL))
                                .withKeySchema(
                                        KeySchemaElement("data", KeyType.HASH),
                                        KeySchemaElement("active", KeyType.RANGE))
                                .withProvisionedThroughput(ProvisionedThroughput(1, 1)))
                .withProvisionedThroughput(ProvisionedThroughput(1, 1))
        client.createTable(createTableRequest)
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val deleteTableRequest = db.generateDeleteTableRequest(ClientRecord::class.java)
        client.deleteTable(deleteTableRequest)
    }
}
