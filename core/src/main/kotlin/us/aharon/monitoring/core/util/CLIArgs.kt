package us.aharon.monitoring.core.util

import org.apache.commons.cli.*
import kotlin.system.exitProcess


internal class CLIArgs(private val args: Array<out String>)  : Options() {

    private val options: CommandLine


    init {
        val printHelp = { exitCode: Int ->
            HelpFormatter().printHelp("java -jar my-app.jar <options>", this)
            exitProcess(exitCode)
        }

        addOption("h", "help", false,
                "This help message")
        addOption("v", "verbose", false,
                "More verbose output")
        addRequiredOption("d", "s3-dest", true,
                "S3 Bucket and path as upload destination. (s3://bucket/path)")
        addRequiredOption("n", "stack-name", true,
                "CloudFormation Stack name")

        try {
            options = DefaultParser().parse(this, args)

            if (options.hasOption("help")) {
                printHelp(0)
            }
        } catch (e: ParseException) {
            println(e.message)
            printHelp(1)
        }
    }

    fun get(value: String): String = this.options.getOptionValue(value)
}
