package com.triptrace.domain.post.post.service;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final TripRepository tripRepository;
    private final ImageRepository imageRepository;
    private final MarkerRepository markerRepository;

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

        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> findPostsByTripId(Long tripId, Long ownerId) {
        Trip trip = tripRepository.findById(tripId)
            .orElseThrow(() -> new ServiceException("404-1", "여행기를 찾을 수 없습니다."));

        if (!trip.isVisibility()) {
            validateOwner(trip, ownerId);
        }

        List<Post> posts = postRepository.findByTripIdOrderByDateAsc(tripId);
        return toResponses(posts);
    }

    @Transactional(readOnly = true)
    public PostResponse findAccessiblePost(Long postId, Long ownerId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ServiceException("404-1", "게시물을 찾을 수 없습니다."));

        if (!post.getTrip().isVisibility()) {
            validateOwner(post.getTrip(), ownerId);
        }

        return toResponse(post);
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

        return toResponse(post);
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

    private PostResponse toResponse(Post post) {
        List<Image> images = imageRepository.findByPostId(post.getId());
        Marker marker = markerRepository.findByPostId(post.getId()).orElse(null);
        return new PostResponse(post, images, marker);
    }

    private List<PostResponse> toResponses(List<Post> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = posts.stream()
            .map(Post::getId)
            .toList();
        Map<Long, List<Image>> imagesByPostId = imageRepository.findByPostIdIn(postIds)
            .stream()
            .collect(Collectors.groupingBy(image -> image.getPost().getId()));
        Map<Long, Marker> markerByPostId = markerRepository.findByPostIdIn(postIds)
            .stream()
            .collect(Collectors.toMap(marker -> marker.getPost().getId(), Function.identity()));

        return posts.stream()
            .map(post -> new PostResponse(
                post,
                imagesByPostId.getOrDefault(post.getId(), List.of()),
                markerByPostId.get(post.getId())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public Post getPost(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(()->new ServiceException("404-1","게시물을 찾을 수 없습니다."));
        return post;
    }

    @Transactional(readOnly = true)
    public Post getPost(Trip trip, Long postId) {
        Post post = getPost(postId);
        if (!post.getTrip().getId().equals(trip.getId())) {
            throw new ServiceException("404-1","게시물을 찾을 수 없습니다.");
        }
        return post;
    }
}
