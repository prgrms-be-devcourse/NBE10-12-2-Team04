package com.triptrace.domain.post.post.service;

import com.triptrace.domain.post.post.dto.PostCreateRequest;
import com.triptrace.domain.post.post.dto.PostModifyRequest;
import com.triptrace.domain.post.post.dto.PostResponse;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final TripRepository tripRepository;

    @Transactional
    public PostResponse create(Long tripId, Long ownerId, PostCreateRequest request) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ServiceException("404-1", "여행기를 찾을 수 없습니다."));
        validateOwner(trip, ownerId);

        Post post = postRepository.save(new Post(
            trip,
            request.date(),
            request.title(),
            request.memo()
        ));

        return new PostResponse(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> findPostsByTripId(Long tripId, Long ownerId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ServiceException("404-1", "여행기를 찾을 수 없습니다."));

        if (!trip.isVisibility()) {
            validateOwner(trip, ownerId);
        }

        return postRepository.findByTripIdOrderByDateAsc(tripId)
            .stream()
            .map(PostResponse::new)
            .toList();
    }

    @Transactional(readOnly = true)
    public PostResponse findAccessiblePost(Long postId, Long ownerId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException("404-1", "게시물을 찾을 수 없습니다."));

        if (!post.getTrip().isVisibility()) {
            validateOwner(post.getTrip(), ownerId);
        }

        return new PostResponse(post);
    }

    @Transactional
    public PostResponse modifyPost(Long postId, Long ownerId, PostModifyRequest request) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException("404-1", "게시물을 찾을 수 없습니다."));
        validateOwner(post.getTrip(), ownerId);

        post.modify(
            request.date(),
            request.title(),
            request.memo()
        );

        return new PostResponse(post);
    }

    @Transactional
    public void deletePost(Long postId, Long ownerId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException("404-1", "게시물을 찾을 수 없습니다."));
        validateOwner(post.getTrip(), ownerId);

        postRepository.delete(post);
    }

    private void validateOwner(Trip trip, Long ownerId) {
        if (!trip.getOwner().getId().equals(ownerId)) {
            throw new ServiceException("403-1", "여행기에 대한 권한이 없습니다.");
        }
    }
}
