/*
 * Copyright (c) 2019 Daniel Aharon
 */

package us.aharon.smdf.core.db

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGenerateStrategy
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGenerator

import java.time.ZonedDateTime


internal class DynamoDBTimestampGenerator : DynamoDBAutoGenerator<ZonedDateTime> {

    override fun generate(currentValue: ZonedDateTime?): ZonedDateTime =
            ZonedDateTime.now()

    override fun getGenerateStrategy(): DynamoDBAutoGenerateStrategy =
            DynamoDBAutoGenerateStrategy.CREATE
}

