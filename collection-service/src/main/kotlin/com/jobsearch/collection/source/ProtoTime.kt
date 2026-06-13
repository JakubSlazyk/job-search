package com.jobsearch.collection.source

import com.google.protobuf.Timestamp
import java.time.Instant

/** Builds a Protobuf [Timestamp] from an [Instant] (defaults to now). */
internal fun timestampOf(instant: Instant = Instant.now()): Timestamp =
    Timestamp
        .newBuilder()
        .setSeconds(instant.epochSecond)
        .setNanos(instant.nano)
        .build()
