package com.triptrace.domain.trip.tripAuto.service;

import com.triptrace.domain.image.image.entity.Image;
import com.triptrace.domain.image.image.repository.ImageRepository;
import com.triptrace.domain.marker.marker.entity.Marker;
import com.triptrace.domain.marker.marker.entity.MarkerSource;
import com.triptrace.domain.marker.marker.repository.MarkerRepository;
import com.triptrace.domain.post.post.entity.Post;
import com.triptrace.domain.post.post.repository.PostRepository;
import com.triptrace.domain.trip.trip.entity.Trip;
import com.triptrace.domain.trip.trip.repository.TripRepository;
import com.triptrace.domain.trip.tripAuto.dto.TripAutoRecordResponse;
import com.triptrace.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
public class TripAutoRecordService {

}
