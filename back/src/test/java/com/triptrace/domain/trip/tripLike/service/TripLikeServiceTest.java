package com.triptrace.domain.trip.tripLike.service;

import com.triptrace.domain.trip.tripLike.repository.TripLikeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TripLikeServiceTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private TripLikeRepository tripLikeRepository;

    @Test
    @DisplayName("좋아요 추가")
    void t1() throws Exception {
        ResultActions resultActions = mvc
            .perform(


            )
    }
}
