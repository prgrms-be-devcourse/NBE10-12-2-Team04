package com.triptrace.domain.post.post.dto;

import java.time.LocalDateTime;

public record PostModifyRequest(
    String title,
    String country,
    String city,
    LocalDateTime startDate,
    LocalDateTime endDate,
    boolean visibility
) {
}
