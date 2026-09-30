package com.socialmedia.postservice.service;

import com.socialmedia.postservice.config.RabbitMQConfig;
import com.socialmedia.postservice.dto.CommentRequest;
import com.socialmedia.postservice.dto.CreatePostRequest;
import com.socialmedia.postservice.dto.PostDTO;
import com.socialmedia.postservice.messaging.PostCreatedEvent;
import com.socialmedia.postservice.model.Comment;
import com.socialmedia.postservice.model.Post;
import com.socialmedia.postservice.repository.CommentRepository;
import com.socialmedia.postservice.repository.PostRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Core post logic. Every successful createPost() publishes a
 * PostCreatedEvent to RabbitMQ so notification-service and
 * analytics-service can react asynchronously - this is the event-driven
 * backbone described in the platform's technical requirements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AmqpTemplate amqpTemplate;

    public PostDTO createPost(CreatePostRequest request, Long authorId, String authorUsername) {
        Post post = Post.builder()
                .authorId(authorId)
                .authorUsername(authorUsername)
                .content(request.getContent())
                .mediaUrl(request.getMediaUrl())
                .build();
        post = postRepository.save(post);

        publishPostCreatedEvent(post);

        return toDTO(post, null);
    }

    @CircuitBreaker(fallbackMethod = "publishFallback")
    public void publishPostCreatedEvent(Post post) {
        PostCreatedEvent event = new PostCreatedEvent(
                post.getId(), post.getAuthorId(), post.getAuthorUsername(),
                post.getContent().length() > 100 ? post.getContent().substring(0, 100) : post.getContent(),
                post.getCreatedAt());
        amqpTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_POST_CREATED, event);
        log.info("Published post.created event for post {}", post.getId());
    }

    // Resilience4j fallback - keeps post creation succeeding even if RabbitMQ is briefly unavailable.
    public void publishFallback(Post post, Throwable t) {
        log.warn("RabbitMQ unavailable, post.created event for post {} was NOT published: {}",
                post.getId(), t.getMessage());
    }

    public Page<PostDTO> getFeed(Pageable pageable, Long currentUserId) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(p -> toDTO(p, currentUserId));
    }

    public Page<PostDTO> getPostsByAuthor(Long authorId, Pageable pageable, Long currentUserId) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable)
                .map(p -> toDTO(p, currentUserId));
    }

    public PostDTO likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (!post.getLikedByUserIds().contains(userId)) {
            post.getLikedByUserIds().add(userId);
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
        }
        return toDTO(post, userId);
    }

    public void unlikePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
        if (post.getLikedByUserIds().remove(userId)) {
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
        }
    }

    public Comment addComment(Long postId, CommentRequest request, Long authorId, String authorUsername) {
        if (!postRepository.existsById(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        }
        Comment comment = Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .authorUsername(authorUsername)
                .content(request.getContent())
                .build();
        return commentRepository.save(comment);
    }

    public java.util.List<Comment> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    private PostDTO toDTO(Post post, Long currentUserId) {
        return PostDTO.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .authorUsername(post.getAuthorUsername())
                .content(post.getContent())
                .mediaUrl(post.getMediaUrl())
                .likeCount(post.getLikeCount())
                .likedByCurrentUser(currentUserId != null && post.getLikedByUserIds().contains(currentUserId))
                .createdAt(post.getCreatedAt())
                .build();
    }
}
