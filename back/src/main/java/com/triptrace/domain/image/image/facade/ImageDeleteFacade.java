package com.triptrace.domain.image.image.facade;

import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageDeleteFacade {
    private final ImageService imageService;
    private final TripService tripService;

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    private Member getMember(Long ownerId){
        return memberRepository.findById(ownerId).orElseThrow(()->new ServiceException("404-1","사용자가 없습니다."));
    }

    @Transactional
    public void deleteByUrl(Long ownerId, Long tripId, Long postId, String imageUrl) {
        Member owner = getMember(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, ownerId);
        Post post = postRepository.findById(postId)
            .orElseThrow(()->new ServiceException("404-1", "게시글을 찾을 수 없습니다."));
        imageService.delete(owner, trip, post, imageUrl);
    }

    @Transactional
    public void deleteById(Long ownerId, Long tripId, Long postId, Long imageId) {
        Member owner = getMember(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, ownerId);
        Post post = postRepository.findById(postId)
            .orElseThrow(()->new ServiceException("404-1", "게시글을 찾을 수 없습니다."));
        imageService.delete(owner, trip, post, imageId);
    }
}
