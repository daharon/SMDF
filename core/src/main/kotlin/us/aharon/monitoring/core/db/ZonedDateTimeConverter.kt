/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


class ZonedDateTimeConverter : DynamoDBTypeConverter<String, ZonedDateTime> {

    override fun convert(dateTime: ZonedDateTime): String =
            dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    override fun unconvert(iso8601: String): ZonedDateTime =
            ZonedDateTime.parse(iso8601, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}
