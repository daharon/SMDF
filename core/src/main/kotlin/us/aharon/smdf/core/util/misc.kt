/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.util

import com.amazonaws.util.BinaryUtils
import com.amazonaws.util.Md5Utils

import us.aharon.smdf.core.api.Application

import java.io.File
import kotlin.reflect.KClass


internal fun getJarFilename(clazz: KClass<out Application>): String =
        File(clazz::class.java.protectionDomain.codeSource.location.path).name

internal fun getJarAbsolutePath(clazz: KClass<out Application>): String =
        File(clazz::class.java.protectionDomain.codeSource.location.path).absolutePath

/**
 * Format [List] to satisfy SNS Message Attribute value for type `String.Array`.
 */
internal fun List<String>.joinToSNSMessageAttributeStringValue(): String =
        this.joinToString("\", \"", "[\"", "\"]")

/**
 * Convert to an MD5 hexadecimal [String].
 */
internal fun String.toMd5HexString(): String {
    val hash = Md5Utils.computeMD5Hash(this.toByteArray())
    return BinaryUtils.toHex(hash).toUpperCase()
}
