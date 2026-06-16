package com.jobsearch.notification.delivery

import com.jobsearch.notification.OfferMatch
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

/**
 * Sends a match notification email. JavaMailSender is blocking, so the send is offloaded to the
 * bounded-elastic scheduler to keep the event loop free. Locally every message is captured by Mailpit.
 */
@Component
class EmailSender(
    private val mailSender: MailSender,
    @param:Value("\${notification.mail.from}") private val from: String,
) {
    fun send(
        to: String,
        displayName: String,
        match: OfferMatch,
    ): Mono<Void> =
        Mono
            .fromRunnable<Void> {
                val message =
                    SimpleMailMessage().apply {
                        this.from = this@EmailSender.from
                        setTo(to)
                        subject = "New matching offer: ${match.title}"
                        text =
                            buildString {
                                appendLine("Hi $displayName,")
                                appendLine()
                                appendLine("A new offer matches your criteria:")
                                appendLine()
                                appendLine("${match.title} — ${match.company}")
                                appendLine(match.url)
                            }
                    }
                mailSender.send(message)
            }.subscribeOn(Schedulers.boundedElastic())
            .then()
}
