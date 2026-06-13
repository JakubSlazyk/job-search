plugins {
    id("job-search.kotlin-common")
    id("job-search.protobuf")
}

// .proto contracts under src/main/proto are compiled to Java + Kotlin messages and gRPC/grpc-kotlin
// stubs by the job-search.protobuf convention plugin. Generated types live under
// com.jobsearch.proto.** (excluded from the JaCoCo gate). See ADR 0003.
