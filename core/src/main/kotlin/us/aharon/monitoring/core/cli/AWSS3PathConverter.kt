/*
 * Copyright (c) 2018 Daniel Aharon
 */

package us.aharon.monitoring.core.cli

import picocli.CommandLine.ITypeConverter

import java.nio.file.Paths


internal data class S3BucketAndPath(
        val bucket: String,
        val path: String
) {
    val combined: String
        get() = "$bucket/$path"
}


/**
 * Sanitize/normalise the <bucket>/<path> provided by the user.
 */
internal class AWSS3PathConverter : ITypeConverter<S3BucketAndPath> {

    override fun convert(bucketAndPath: String): S3BucketAndPath {
        val parts = Paths.get(bucketAndPath)
                .normalize().toString()
                .split('/', limit = 2)
        return S3BucketAndPath(
                bucket = parts.first(),
                path = parts.getOrElse(1) { "" }
        )
    }
}

