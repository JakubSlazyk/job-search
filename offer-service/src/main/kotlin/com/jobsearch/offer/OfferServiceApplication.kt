package com.jobsearch.offer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class OfferServiceApplication

fun main(args: Array<String>) {
    runApplication<OfferServiceApplication>(*args)
}
