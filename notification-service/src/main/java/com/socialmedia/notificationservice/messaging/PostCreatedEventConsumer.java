package com.socialmedia.notificationservice.messaging;

import com.socialmedia.notificationservice.config.RabbitMQConfig;
import com.socialmedia.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens on notification.queue (bound to social-media.exchange with
 * routing key "post.created"). This is the async, decoupled counterpart
 * to post-service's PostService.publishPostCreatedEvent().
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostCreatedEventConsumer {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handlePostCreatedEvent(PostCreatedEvent event) {
        log.info("Received post.created event for post {}", event.getPostId());
        notificationService.notifyFollowersOfNewPost(
                event.getAuthorId(), event.getAuthorUsername(), event.getPostId());
    }
}
