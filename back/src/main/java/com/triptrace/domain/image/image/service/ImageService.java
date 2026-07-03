package com.triptrace.domain.image.image.service;

import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.trip.trip.entity.Trip;

import java.util.List;

public interface ImageService {
    ImageServiceResponse create(Image image);

    ImageServiceResponse findById(Long id);
    ImageServiceResponse findByUrl(String imageUrl);
    List<ImageServiceResponse> findByTripId(Trip trip);
    List<ImageServiceResponse> findByPostId(Post post);

    ImageServiceResponse delete(Member owner, Trip trip, Post post, Long id);
    ImageServiceResponse delete(Member owner, Trip trip, Post post, String imageUrl);

    ImageServiceResponse modifyPost(Member owner, Trip tripId, Post post, Long imageId);
}
