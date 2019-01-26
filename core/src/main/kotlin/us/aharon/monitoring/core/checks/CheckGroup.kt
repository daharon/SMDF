/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.checks

import kotlin.reflect.full.primaryConstructor


@DslMarker
annotation class CheckDslMarker


@CheckDslMarker
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
