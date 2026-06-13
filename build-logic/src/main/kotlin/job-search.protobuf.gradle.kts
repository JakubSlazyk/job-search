import com.google.protobuf.gradle.ProtobufExtension
import com.google.protobuf.gradle.id
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.SourceTask
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

// Protobuf + gRPC codegen wiring for modules that hold .proto contracts (currently common-domain).
// Layered on top of job-search.kotlin-common — the module applies both. Protobuf is the single IDL
// for Kafka event payloads and internal gRPC, per ADR 0003.
//
// Convention: every generated contract type lives under the `com.jobsearch.proto.**` package
// (set via `option java_package` in each .proto). That package is generated, never hand-written,
// so it is excluded from the JaCoCo coverage gate and from detekt below.

plugins {
    id("com.google.protobuf")
}

val libs = the<LibrariesForLibs>()

configure<ProtobufExtension> {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${libs.versions.grpcKotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        // protoc emits Java by default; add the Kotlin DSL builders and both gRPC stub flavours.
        all().forEach { task ->
            task.builtins {
                id("kotlin")
            }
            task.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}

dependencies {
    // String-keyed configs: the kotlin/java accessors aren't generated for this plugin's own
    // classpath, but the module also applies kotlin-common, so `implementation` exists at use time.
    "implementation"(libs.protobuf.java)
    "implementation"(libs.protobuf.kotlin)
    "implementation"(libs.grpc.stub)
    "implementation"(libs.grpc.protobuf)
    "implementation"(libs.grpc.kotlin.stub)
}

// Keep generated code out of the coverage gate (it isn't hand-written).
tasks.withType<JacocoReport>().configureEach {
    classDirectories.setFrom(
        classDirectories.files.map { dir -> fileTree(dir) { exclude("com/jobsearch/proto/**") } },
    )
}
tasks.withType<JacocoCoverageVerification>().configureEach {
    classDirectories.setFrom(
        classDirectories.files.map { dir -> fileTree(dir) { exclude("com/jobsearch/proto/**") } },
    )
}

// Keep generated sources out of static analysis (detekt tasks are SourceTasks).
tasks.matching { it.name.startsWith("detekt") }.configureEach {
    if (this is SourceTask) {
        exclude("**/build/generated/**")
    }
}
