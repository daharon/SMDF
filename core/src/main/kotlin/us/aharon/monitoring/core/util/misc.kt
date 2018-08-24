/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.util

import us.aharon.monitoring.core.Application

import java.io.File
import kotlin.reflect.KClass


internal fun getJarFilename(clazz: KClass<out Application>): String =
        File(clazz::class.java.protectionDomain.codeSource.location.path).name

internal fun getJarAbsolutePath(clazz: KClass<out Application>): String =
        File(clazz::class.java.protectionDomain.codeSource.location.path).absolutePath
