package com.triptrace.domain.image.image.application;

import com.triptrace.domain.image.image.catalog.ImageExceptionCatalog;
import com.triptrace.domain.image.image.dto.response.ImageServiceResponse;
import com.triptrace.domain.image.image.service.ImageService;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.service.MemberService;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.service.PostService;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageModifyUseCase {
    private final ImageService imageService;
    private final TripService tripService;
    private final PostService postService;
    private final MemberService memberService;

    @Transactional
    public ImageServiceResponse modifyById(Long ownerId, Long tripId, Long postId, Long imageId) {
        Member owner = memberService.findById(ownerId);
        Trip trip = tripService.findOwnedTrip(tripId, owner.getId());
        Post post = postService.getPost(trip, postId);
        if (!post.getTrip().getId().equals(trip.getId())) {
            throw ImageExceptionCatalog.invalid();
        }
        return imageService.modifyPost(owner, trip, post, imageId);
    }

    public ImageServiceResponse unassign(Long ownerId, Long tripId, Long imageId) {
        ImageServiceResponse imageServiceResponse = imageService.unassign(ownerId,tripId, imageId);
        return imageServiceResponse;

    }
}
