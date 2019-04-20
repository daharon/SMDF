/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import cloud.localstack.LocalstackExtension
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import mu.KLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.koin.core.parameter.parametersOf
import org.koin.standalone.inject
import org.koin.test.KoinTest

import us.aharon.monitoring.core.extensions.DynamoDBTableExtension
import us.aharon.monitoring.core.extensions.LoadModulesExtension

import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@Extensions(
    ExtendWith(LocalstackExtension::class),
    ExtendWith(LoadModulesExtension::class),
    ExtendWith(DynamoDBTableExtension::class))
class NotificationRecordTest : KoinTest {

    private val log: KLogger by inject { parametersOf(this::class.java.simpleName) }

    @Test
    fun `Sort key is generated correctly`() {
        val db: DynamoDBMapper by inject()

        val checkGroup = "test"
        val checkName = "Test Check"
        val source = "server-1.example.com"
        val resultCompletedAt = ZonedDateTime.parse("2018-08-23T11:39:44Z")
        val notificationInput = NotificationRecord(
                handler = "com.example.Notifier",
                checkGroup = checkGroup,
                checkName = checkName,
                source = source,
                resultId = "xyz789",
                resultCompletedAt = resultCompletedAt,
                description = "Notification sent.")
        db.save(notificationInput)

        // Query the saved notification and check the value of its sort key.
        val query = DynamoDBQueryExpression<NotificationRecord>()
                .withKeyConditionExpression("#pk = :notifId")
                .withExpressionAttributeNames(mapOf("#pk" to "pk"))
                .withExpressionAttributeValues(mapOf(
                        ":notifId" to AttributeValue(NotificationRecord.generateNotificationId(checkGroup, checkName))))
                .withConsistentRead(true)
                .withLimit(1)
        val notification = db.query(NotificationRecord::class.java, query).firstOrNull()
        log.info("Notification sort key as read from the database:  ${notification?.notificationSortKey}")
        assertNotNull(notification)
        val sortKeyParts = notification.notificationSortKey?.split('|')
        assertEquals(source, sortKeyParts?.get(1))
        assertEquals(resultCompletedAt, ZonedDateTime.parse(sortKeyParts?.get(0)))
    }
}
