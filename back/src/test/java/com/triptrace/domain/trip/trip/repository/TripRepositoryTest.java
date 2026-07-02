package com.triptrace.domain.trip.trip.repository;

import com.triptrace.domain.member.member.entity.Member;
import com.triptrace.domain.member.member.entity.MemberStatus;
import com.triptrace.domain.member.member.repository.MemberRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.tripLike.service.TripLikeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class TripRepositoryTest {
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TripLikeService tripLikeService;
    private Member member1;
    private Trip trip1;

    @BeforeEach
    public void setup() {
        member1 = memberRepository.save(new Member(
            "member1@test.com",
            "member1",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member2 = memberRepository.save(new Member(
            "member2@test.com",
            "member2",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member3 = memberRepository.save(new Member(
            "member3@test.com",
            "member3",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member4 = memberRepository.save(new Member(
            "member4@test.com",
            "member4",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member5 = memberRepository.save(new Member(
            "member5@test.com",
            "member5",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member6 = memberRepository.save(new Member(
            "member6@test.com",
            "member6",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member7 = memberRepository.save(new Member(
            "member7@test.com",
            "member7",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member8 = memberRepository.save(new Member(
            "member8@test.com",
            "member8",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member9 = memberRepository.save(new Member(
            "member9@test.com",
            "member9",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member member10 = memberRepository.save(new Member(
            "member10@test.com",
            "member10",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner1 = memberRepository.save(new Member(
            "owner1@test.com",
            "owner1",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner2 = memberRepository.save(new Member(
            "owner2@test.com",
            "owner2",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner3 = memberRepository.save(new Member(
            "owner3@test.com",
            "owner3",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner4 = memberRepository.save(new Member(
            "owner4@test.com",
            "owner4",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner5 = memberRepository.save(new Member(
            "owner5@test.com",
            "owner5",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner6 = memberRepository.save(new Member(
            "owner6@test.com",
            "owner6",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner7 = memberRepository.save(new Member(
            "owner7@test.com",
            "owner7",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner8 = memberRepository.save(new Member(
            "owner8@test.com",
            "owner8",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner9 = memberRepository.save(new Member(
            "owner9@test.com",
            "owner9",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Member owner10 = memberRepository.save(new Member(
            "owner10@test.com",
            "owner10",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        trip1 = tripRepository.save(new Trip(
            owner1,
            "title1",
            "country1",
            "city1",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip2 = tripRepository.save(new Trip(
            owner2,
            "title2",
            "country2",
            "city2",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip3 = tripRepository.save(new Trip(
            owner3,
            "title3",
            "country3",
            "city3",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip4 = tripRepository.save(new Trip(
            owner4,
            "title4",
            "country4",
            "city3",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip5 = tripRepository.save(new Trip(
            owner5,
            "title5",
            "country5",
            "city5",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip6 = tripRepository.save(new Trip(
            owner6,
            "title6",
            "country6",
            "city6",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip7 = tripRepository.save(new Trip(
            owner7,
            "title7",
            "country7",
            "city7",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip8 = tripRepository.save(new Trip(
            owner8,
            "title8",
            "country8",
            "city8",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip9 = tripRepository.save(new Trip(
            owner9,
            "title9",
            "country9",
            "city9",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        Trip trip10 = tripRepository.save(new Trip(
            owner10,
            "title10",
            "country10",
            "city10",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            true
        ));

        tripLikeService.createLike(member1.getId(), trip10.getId());
        tripLikeService.createLike(member2.getId(), trip10.getId());
        tripLikeService.createLike(member3.getId(), trip10.getId());
        tripLikeService.createLike(member4.getId(), trip10.getId());
        tripLikeService.createLike(member5.getId(), trip10.getId());
        tripLikeService.createLike(member6.getId(), trip10.getId());
        tripLikeService.createLike(member7.getId(), trip10.getId());
        tripLikeService.createLike(member8.getId(), trip10.getId());
        tripLikeService.createLike(member9.getId(), trip10.getId());
        tripLikeService.createLike(member10.getId(), trip10.getId());
        tripLikeService.createLike(member1.getId(), trip9.getId());
        tripLikeService.createLike(member2.getId(), trip9.getId());
        tripLikeService.createLike(member3.getId(), trip9.getId());
        tripLikeService.createLike(member4.getId(), trip9.getId());
        tripLikeService.createLike(member5.getId(), trip9.getId());
        tripLikeService.createLike(member6.getId(), trip9.getId());
        tripLikeService.createLike(member7.getId(), trip9.getId());
        tripLikeService.createLike(member8.getId(), trip9.getId());
        tripLikeService.createLike(member9.getId(), trip9.getId());
        tripLikeService.createLike(member1.getId(), trip8.getId());
        tripLikeService.createLike(member2.getId(), trip8.getId());
        tripLikeService.createLike(member3.getId(), trip8.getId());
        tripLikeService.createLike(member4.getId(), trip8.getId());
        tripLikeService.createLike(member5.getId(), trip8.getId());
        tripLikeService.createLike(member6.getId(), trip8.getId());
        tripLikeService.createLike(member7.getId(), trip8.getId());
        tripLikeService.createLike(member8.getId(), trip8.getId());
        tripLikeService.createLike(member1.getId(), trip7.getId());
        tripLikeService.createLike(member2.getId(), trip7.getId());
        tripLikeService.createLike(member3.getId(), trip7.getId());
        tripLikeService.createLike(member4.getId(), trip7.getId());
        tripLikeService.createLike(member5.getId(), trip7.getId());
        tripLikeService.createLike(member6.getId(), trip7.getId());
        tripLikeService.createLike(member7.getId(), trip7.getId());
        tripLikeService.createLike(member1.getId(), trip6.getId());
        tripLikeService.createLike(member2.getId(), trip6.getId());
        tripLikeService.createLike(member3.getId(), trip6.getId());
        tripLikeService.createLike(member4.getId(), trip6.getId());
        tripLikeService.createLike(member5.getId(), trip6.getId());
        tripLikeService.createLike(member6.getId(), trip6.getId());
        tripLikeService.createLike(member1.getId(), trip5.getId());
        tripLikeService.createLike(member2.getId(), trip5.getId());
        tripLikeService.createLike(member3.getId(), trip5.getId());
        tripLikeService.createLike(member4.getId(), trip5.getId());
        tripLikeService.createLike(member5.getId(), trip5.getId());
        tripLikeService.createLike(member1.getId(), trip4.getId());
        tripLikeService.createLike(member2.getId(), trip4.getId());
        tripLikeService.createLike(member3.getId(), trip4.getId());
        tripLikeService.createLike(member4.getId(), trip4.getId());
        tripLikeService.createLike(member1.getId(), trip3.getId());
        tripLikeService.createLike(member2.getId(), trip3.getId());
        tripLikeService.createLike(member3.getId(), trip3.getId());
        tripLikeService.createLike(member1.getId(), trip2.getId());
        tripLikeService.createLike(member2.getId(), trip2.getId());
        tripLikeService.createLike(member1.getId(), trip1.getId());
    }

    @Test
    @DisplayName("좋아요 상위 10개 조회 테스트")
    public void t1() {
        List<Trip> tripList = tripRepository.findTop10ByVisibilityTrueOrderByLikeCountDesc();
        System.out.println(tripList.get(0).getLikeCount());
        System.out.println(tripList.get(1).getLikeCount());
        System.out.println(tripList.get(2).getLikeCount());
        System.out.println(tripList.get(3).getLikeCount());
        System.out.println(tripList.get(4).getLikeCount());
        System.out.println(tripList.get(5).getLikeCount());
        System.out.println(tripList.get(6).getLikeCount());
        System.out.println(tripList.get(7).getLikeCount());
        System.out.println(tripList.get(8).getLikeCount());
        System.out.println(tripList.get(9).getLikeCount());
    }

    @Test
    @DisplayName("좋아요가 있는 비공개여행기의 경우 좋아요 상위 10개 조회시 조회여부 테스트")
    public void t2() throws Exception {
        Member owner11 = memberRepository.save(new Member(
            "owner11@test.com",
            "owner11",
            "password1234",
            "imageUrl",
            MemberStatus.ACTIVE
        ));

        Trip trip11 = tripRepository.save(new Trip(
            owner11,
            "title11",
            "country11",
            "city11",
            LocalDateTime.now().minusMonths(12),
            LocalDateTime.now().minusMonths(6),
            false
        ));

        tripLikeService.deleteLike(member1.getId(), trip1.getId());

        tripLikeService.createLike(member1.getId(), trip11.getId());

        List<Trip> tripList = tripRepository.findTop10ByVisibilityTrueOrderByLikeCountDesc();

        System.out.println(tripList.get(0).getLikeCount());
        System.out.println(tripList.get(1).getLikeCount());
        System.out.println(tripList.get(2).getLikeCount());
        System.out.println(tripList.get(3).getLikeCount());
        System.out.println(tripList.get(4).getLikeCount());
        System.out.println(tripList.get(5).getLikeCount());
        System.out.println(tripList.get(6).getLikeCount());
        System.out.println(tripList.get(7).getLikeCount());
        System.out.println(tripList.get(8).getLikeCount());
        System.out.println(tripList.get(9).getLikeCount());
        System.out.println(tripList.getLast().getId());
    }
}
