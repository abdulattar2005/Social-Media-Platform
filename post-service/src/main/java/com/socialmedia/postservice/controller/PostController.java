package com.socialmedia.postservice.controller;

import com.socialmedia.postservice.dto.CommentRequest;
import com.socialmedia.postservice.dto.CreatePostRequest;
import com.socialmedia.postservice.dto.PostDTO;
import com.socialmedia.postservice.model.Comment;
import com.socialmedia.postservice.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostDTO> createPost(@Valid @RequestBody CreatePostRequest request,
                                               @RequestHeader("X-Auth-User") String username,
                                               @RequestParam Long authorId) {
        PostDTO post = postService.createPost(request, authorId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    @GetMapping
    public Page<PostDTO> getFeed(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size);
        return postService.getFeed(pageable, currentUserId);
    }

    @GetMapping("/author/{authorId}")
    public Page<PostDTO> getByAuthor(@PathVariable Long authorId,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) Long currentUserId) {
        return postService.getPostsByAuthor(authorId, PageRequest.of(page, size), currentUserId);
    }

    @PostMapping("/{id}/like")
    public PostDTO like(@PathVariable Long id, @RequestParam Long userId) {
        return postService.likePost(id, userId);
    }

    @DeleteMapping("/{id}/like")
    public void unlike(@PathVariable Long id, @RequestParam Long userId) {
        postService.unlikePost(id, userId);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> comment(@PathVariable Long id, @Valid @RequestBody CommentRequest request,
                                            @RequestHeader("X-Auth-User") String username,
                                            @RequestParam Long authorId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.addComment(id, request, authorId, username));
    }

    @GetMapping("/{id}/comments")
    public List<Comment> getComments(@PathVariable Long id) {
        return postService.getComments(id);
    }
}
