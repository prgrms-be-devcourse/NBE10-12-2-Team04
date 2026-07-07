package com.triptrace.domain.post.post.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record PostModifyRequest(
    LocalDate date,
    LocalTime time,
    @Size(max = 100)
    String title,
    String memo
) {
}
