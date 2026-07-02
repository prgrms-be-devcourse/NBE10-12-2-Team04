package com.triptrace.domain.image.image.facade;

import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.dto.PostResponse;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageModifyFacade {
    private final ImageService imageService;
    private final TripService tripService;
    private final PostService postService;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    public Member getMember(Long memberId){
        return memberRepository.findById(memberId)
            .orElseThrow(()->new ServiceException("404-1","회원을 찾을 수 없습니다."));
    }
    @Transactional
    public ImageServiceResponse modifyById(Long ownerId, Long tripId, Long postId, Long imageId) {
        Member owner = getMember(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        PostResponse postResponse = postService.findPostsByTripId(tripId,ownerId)
            .stream()
            .filter((p)->p.id().equals(postId))
            .findFirst()
            .orElseThrow(()-> new ServiceException("404-1","게시글을 찾을 수 없습니다."));
        Post post = postRepository.findById(postId).orElseThrow(()->new ServiceException("404-1","게시글을 찾을 수 없습니다."));
        return imageService.modifyPost(owner, trip, post, imageId);
    }
}
