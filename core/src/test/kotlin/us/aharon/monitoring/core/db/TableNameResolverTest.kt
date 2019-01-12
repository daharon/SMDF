/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import us.aharon.monitoring.core.extensions.LoadModulesExtension
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


@ExtendWith(LoadModulesExtension::class)
class TableNameResolverTest {

    @Test
    fun `Table name resolves successfully`() {
        val dynamoDbTableRecord = ClientRecord()
        val expected = "FAKE_DB_TABLE"
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
