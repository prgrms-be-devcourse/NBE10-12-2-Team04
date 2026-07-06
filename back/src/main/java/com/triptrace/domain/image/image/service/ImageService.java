package com.triptrace.domain.image.image.service;

import com.triptrace.domain.image.image.catalog.ImageExceptionCatalog;
import com.triptrace.domain.image.image.dto.ImageServiceResponse;
import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.factory.ImageFactory;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.trip.trip.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;

    @Transactional
    public ImageServiceResponse create(Image image) {
        image = imageRepository.save(image);
        return ImageFactory.createImageServiceResponse(image);
    }
    @Transactional
    public ImageServiceResponse delete(Image image) {
        //외부 접근 제외
        ImageServiceResponse response = ImageFactory.createImageServiceResponse(image);
        imageRepository.delete(image);
        return response;
    }
    @Transactional
    public ImageServiceResponse modifyPost(Member owner, Trip trip, Post post, Long imageId) {
        Image image = getById(imageId);
        if( ! validateOwner(owner, image)){
            throw ImageExceptionCatalog.forbidden();
        }
        if( ! validateTrip(trip, image)) {
            throw ImageExceptionCatalog.invalid("해당 여행기의 이미지가 아닙니다.");
        }
        image.modifyPost(post);
        image = imageRepository.save(image);
        return ImageFactory.createImageServiceResponse(image);
    }
    @Transactional
    public ImageServiceResponse delete(Member owner, Trip trip, Post post, Long id) {
        Image image = getById(id);
        validate(owner, trip, post, image);
        return delete(image);
    }

    @Transactional
    public ImageServiceResponse delete(Member owner, Trip trip, Post post, String imageUrl) {
        Image image = getByUrl(imageUrl);
        validate(owner, trip, post, image);
        return delete(image);
    }

    private Image getById(Long id) {
        Image image = imageRepository.findById(id).orElseThrow(
            ImageExceptionCatalog::notFound);
        return image;
    }


    private Image getByUrl(String originalFileUrl) {
        Image image = imageRepository.findByOriginalFileUrl(originalFileUrl)
            .orElseThrow(ImageExceptionCatalog::notFound);
        return image;
    }

    @Transactional(readOnly = true)
    public ImageServiceResponse findById(Long id){
        Image image = getById(id);
        return ImageFactory.createImageServiceResponse(image);
    }

    @Transactional(readOnly = true)
    public ImageServiceResponse findByUrl(String imageUrl) {
        Image image = getByUrl(imageUrl);
        return ImageFactory.createImageServiceResponse(image);
    }

    @Transactional(readOnly = true)
    public List<ImageServiceResponse> findByTripId(Trip trip) {
        //TODO: 고려사항 pagination?
        List<Image> image = imageRepository.findByTripId(trip.getId());
        return image.stream().map(ImageFactory::createImageServiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ImageServiceResponse> findByPostId(Post post) {
        List<Image> image = imageRepository.findByPostId(post.getId());
        return image.stream().map(ImageFactory::createImageServiceResponse).toList();
    }


    private void validate(Member owner, Trip trip, Post post, Image image) {
        if( ! validateOwner(owner,image)){
            throw ImageExceptionCatalog.forbidden();
        }
        if( ! validateTrip(trip,image)){
            throw ImageExceptionCatalog.invalid("해당 여행기의 이미지가 아닙니다.");
        }
        if( ! validatePost(post,image)){
            throw ImageExceptionCatalog.forbidden("해당 게시글의 이미지가 아닙니다.");
        }
    }

    private boolean validateOwner(Member owner, Image image){
        if(owner.getId().equals(image.getOwner().getId())){
            return true;
        }
        return false;
    }

    private boolean validateTrip(Trip trip, Image image){
        if(trip.getId().equals(image.getTrip().getId())){
            return true;
        }
        return false;
    }
    private boolean validatePost(Post post, Image image){
        if(post == null){
            return true;
        }
        if(image.getPost() == null){
            return false;
        }
        return post.getId().equals(image.getPost().getId());
    }
}
