package com.triptrace.domain.image.image.facade;

import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.member.member.service.MemberService;
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
    private final PostRepository postRepository;
    private final MemberService memberService;

    //profile 이미지의 삭제 방법
    //image 삭제시 파일도 삭제해야하는가?
    //post를 거치지 않는 이미지는 어떤 이미지?

    @Transactional
    public void deleteByUrl(String email, Long tripId, Long postId, String imageUrl) {
        Member owner = memberService.findByEmail(email);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postRepository.findById(postId)
            .orElseThrow(()->new ServiceException("404-1", "게시글을 찾을 수 없습니다."));
        imageService.delete(owner, trip, post, imageUrl);
    }

    @Transactional
    public void deleteById(String email, Long tripId, Long postId, Long imageId) {
        Member owner = memberService.findByEmail(email);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postRepository.findById(postId)
            .orElseThrow(()->new ServiceException("404-1", "게시글을 찾을 수 없습니다."));
        imageService.delete(owner, trip, post, imageId);
    }
}
