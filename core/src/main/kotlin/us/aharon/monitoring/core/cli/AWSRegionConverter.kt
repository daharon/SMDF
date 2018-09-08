/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.cli

import com.amazonaws.regions.Regions
import picocli.CommandLine.ITypeConverter


/**
 * Type converter for the commandline parser.
 */
internal class AWSRegionConverter : ITypeConverter<Regions> {

    override fun convert(region: String): Regions = Regions.fromName(region)
}
