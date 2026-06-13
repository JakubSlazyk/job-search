package com.jobsearch.common.domain

import com.jobsearch.proto.example.v1.Ping
import com.jobsearch.proto.example.v1.ping as buildPing

/**
 * Hand-written helper that builds a generated Protobuf message via the Kotlin DSL.
 *
 * Its only job for now is to exercise the Protobuf + gRPC codegen wiring (the job-search.protobuf
 * convention plugin) end-to-end and to give the JaCoCo gate real, non-generated code to measure.
 * Replaced by genuine domain contracts as the pipeline schemas land in Phase 1 (see ADR 0003).
 */
object ProtoContracts {
    fun newPing(text: String): Ping = buildPing { message = text }
}
