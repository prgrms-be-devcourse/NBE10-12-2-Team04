package com.triptrace.domain.post.post.controller;

import com.triptrace.domain.post.post.dto.PostCreateRequest;
import com.triptrace.domain.post.post.dto.PostModifyRequest;
import com.triptrace.domain.post.post.dto.PostResponse;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiV1PostController {
    private final PostService postService;

    @PostMapping("/trips/{tripId}/posts")
    public RsData<PostResponse> createPost(
        @PathVariable Long tripId,
        @RequestParam Long ownerId, //임시 인증
        @RequestBody @Valid PostCreateRequest request
    ) {
        PostResponse response = postService.create(tripId, ownerId, request);

        return new RsData<>(
            "201-1",
            "%d번 게시물이 생성되었습니다.".formatted(response.id()),
            response
        );
    }

    @GetMapping("/trips/{tripId}/posts")
    public RsData<List<PostResponse>> getPosts(
        @PathVariable Long tripId,
        @RequestParam(required = false) Long ownerId
    ) {
        return new RsData<>(
            "200-1",
            "게시물 목록 조회에 성공했습니다.",
            postService.findPostsByTripId(tripId, ownerId)
        );
    }

    @GetMapping("/posts/{postId}")
    public RsData<PostResponse> getPost(
        @PathVariable Long postId,
        @RequestParam(required = false) Long ownerId
    ) {
        return new RsData<>(
            "200-1",
            "%d번 게시물 조회에 성공했습니다.".formatted(postId),
            postService.findAccessiblePost(postId, ownerId)
        );
    }

    @PatchMapping("/posts/{postId}")
    public RsData<PostResponse> modifyPost(
        @PathVariable Long postId,
        @RequestParam Long ownerId,
        @RequestBody @Valid PostModifyRequest request
    ) {
        return new RsData<>(
            "200-1",
            "%d번 게시물이 수정되었습니다.".formatted(postId),
            postService.modifyPost(postId, ownerId, request)
        );
    }

    @DeleteMapping("/posts/{postId}")
    public RsData<Void> deletePost(
        @PathVariable Long postId,
        @RequestParam Long ownerId
    ) {
        postService.deletePost(postId, ownerId);

        return new RsData<>(
            "200-1",
            "%d번 게시물이 삭제되었습니다.".formatted(postId)
        );
    }
}
