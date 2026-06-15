package com.jobsearch.collection

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CollectionServiceApplication

fun main(args: Array<String>) {
    runApplication<CollectionServiceApplication>(*args)
}
