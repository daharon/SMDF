/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.cli

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option

import us.aharon.monitoring.core.api.Application

import kotlin.system.exitProcess


@Command(name = "java -jar app.jar",
        description = ["Serverless monitoring application"],
        mixinStandardHelpOptions = true,
        subcommands = [Deploy::class])
internal class Base(val app: Application) : Runnable {

    @Option(names = ["-v", "--verbose"],
            description = ["More verbose output"])
    var verbose: Boolean = false

    /**
     * Print the usage instructions and sub-commands.
     */
    override fun run() {
        CommandLine.usage(this, System.out)
        exitProcess(0)
    }
}
