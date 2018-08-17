package us.aharon.monitoring.core.checks


inline fun checks(block: CheckGroup.() -> Unit): CheckGroup =
    CheckGroup().apply(block)

inline fun CheckGroup.check(name: String, block: ClientCheck.() -> Unit) {
    val check = ClientCheck()
    check.apply(block)
    checks.add(check)
}

inline fun CheckGroup.serverlessCheck(name: String, block: ServerlessCheck.() -> Unit) {
    val check = ServerlessCheck()
    check.apply(block)
    checks.add(check)
}

class CheckGroup {

    val checks: MutableList<Check> = mutableListOf()
}
