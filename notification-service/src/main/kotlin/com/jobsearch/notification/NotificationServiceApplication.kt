package com.jobsearch.notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator

@SpringBootApplication
class NotificationServiceApplication {
    // Boot auto-configures the R2dbcTransactionManager but not a TransactionalOperator; the matcher
    // uses it to write the dedup guard + delivery rows atomically (see NotificationService).
    @Bean
    fun transactionalOperator(transactionManager: ReactiveTransactionManager): TransactionalOperator =
        TransactionalOperator.create(transactionManager)
}

fun main(args: Array<String>) {
    runApplication<NotificationServiceApplication>(*args)
}
