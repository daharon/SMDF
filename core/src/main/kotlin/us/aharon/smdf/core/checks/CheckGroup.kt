/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.smdf.core.checks

import us.aharon.smdf.core.handlers.NotificationHandler

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


@DslMarker
@Target(AnnotationTarget.FUNCTION)
annotation class CheckDslMarker


class CheckGroup(val name: String) {

    val checks: MutableList<Check> = mutableListOf()
}


/**
 * Extract the permissions required by each serverless check executor.
 *
 * @return Map of the executors' names to a list of their permissions.
 */
internal fun List<CheckGroup>.serverlessExecutorPermissions(): Map<String, List<Permission>> =
        this.flatMap<CheckGroup, ServerlessCheck> { checkGroup ->
            checkGroup.checks.filterIsInstance(ServerlessCheck::class.java)
        }.associateBy<ServerlessCheck, String, List<Permission>>(
                { it.executor.qualifiedName!! },
                {
                    val constructor = it.executor.primaryConstructor
                    if (constructor != null && constructor.parameters.isEmpty()) {
                        constructor.call().permissions
                    } else {
                        emptyList<Permission>()
                    }
                }
        )

/**
 * Extract the permissions required by the notification handlers in use.
 *
 * @return Map of the handlers' names to a list of their permissions.
 */
internal fun List<CheckGroup>.notificationHandlerPermissions(): Map<String, List<Permission>> =
        this.flatMap<CheckGroup, KClass<out NotificationHandler>> { checkGroup ->
            checkGroup.checks.flatMap { it.handlers }
        }.mapNotNull {
            val constructor = it.primaryConstructor
            if (constructor != null && constructor.parameters.isEmpty()) {
                constructor.call()
            } else {
                null
            }
        }.associateBy<NotificationHandler, String, List<Permission>>(
                { it::class.qualifiedName!! },
                { it.permissions }
        )
