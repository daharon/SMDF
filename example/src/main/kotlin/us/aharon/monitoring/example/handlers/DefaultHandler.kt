package us.aharon.monitoring.example.handlers


/**
 * https://docs.sensu.io/sensu-core/1.4/reference/handlers/#handler-configuration
 */
class DefaultHandler : StandardHandler() {

    override val filters = listOf(
            ::exampleFilter
    )
    override val mutators = listOf(
            ::exampleMutator
    )

    override fun run(event: Event): Unit {

    }
}
