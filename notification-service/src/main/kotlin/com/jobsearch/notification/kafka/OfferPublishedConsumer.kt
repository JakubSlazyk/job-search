package com.jobsearch.notification.kafka

import com.jobsearch.common.domain.Topics
import com.jobsearch.notification.NotificationService
import com.jobsearch.proto.offer.v1.OfferPublished
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord

/**
 * Reactive inbound adapter: a reactor-kafka [KafkaReceiver] streams `offer.published` and each record
 * is handed to [NotificationService]. `concatMap` processes one record at a time (ordered);
 * idempotency lives in the service (dedup on `event_id`), so the offset is acknowledged after every
 * attempt — a redelivery is a harmless no-op. Managed as a [SmartLifecycle] so it starts with the
 * context and is disposed cleanly on shutdown. Disable in slices that have no broker with
 * `notification.kafka.enabled=false`.
 */
@Component
@ConditionalOnProperty(name = ["notification.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class OfferPublishedConsumer(
    @param:Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
    @param:Value("\${spring.kafka.consumer.group-id}") private val groupId: String,
    @param:Value("\${spring.kafka.consumer.auto-offset-reset}") private val autoOffsetReset: String,
    private val notificationService: NotificationService,
) : SmartLifecycle {
    private val log = LoggerFactory.getLogger(javaClass)
    private var subscription: Disposable? = null

    override fun start() {
        subscription =
            receiver()
                .receive()
                .concatMap(::handle)
                .subscribe()
    }

    private fun handle(record: ReceiverRecord<String, ByteArray>): Mono<Void> =
        Mono
            .defer { notificationService.onOfferPublished(OfferPublished.parseFrom(record.value())) }
            .doOnError { error -> log.error("Failed to process offer.published, skipping", error) }
            .onErrorComplete()
            .doFinally { record.receiverOffset().acknowledge() }

    override fun stop() {
        subscription?.dispose()
        subscription = null
    }

    override fun isRunning(): Boolean = subscription?.isDisposed == false

    private fun receiver(): KafkaReceiver<String, ByteArray> {
        val consumerProps =
            mapOf<String, Any>(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            )
        val options =
            ReceiverOptions
                .create<String, ByteArray>(consumerProps)
                .withKeyDeserializer(StringDeserializer())
                .withValueDeserializer(ByteArrayDeserializer())
                .subscription(listOf(Topics.OFFER_PUBLISHED))
        return KafkaReceiver.create(options)
    }
}
