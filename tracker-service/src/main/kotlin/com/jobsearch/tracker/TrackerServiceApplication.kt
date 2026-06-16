package com.jobsearch.tracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TrackerServiceApplication

fun main(args: Array<String>) {
    runApplication<TrackerServiceApplication>(*args)
}
