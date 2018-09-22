/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class TableNameResolverTest {

    @Test
    fun `Table name resolves successfully`() {
        val dynamoDbTableRecord = ClientRecord()
        val expected = ""  // Empty, since we don't have environment variables set.
        val actual = TableNameResolver().getTableName(dynamoDbTableRecord::class.java, null)
        assertEquals(expected, actual)
    }

    @Test
    fun `Table name resolution fails on unknown class`() {
        @DynamoDBTable(tableName = "DYNAMICALLY_DEFINED")
        data class UnknownRecord(val x: Int?)

        val dynamoDbTableRecord = UnknownRecord(0)
        assertFailsWith(DynamoDBMappingException::class) {
            TableNameResolver().getTableName(dynamoDbTableRecord::class.java, null)
        }
    }
}
