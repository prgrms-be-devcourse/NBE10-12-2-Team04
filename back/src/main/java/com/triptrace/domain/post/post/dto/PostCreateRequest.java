package com.triptrace.domain.post.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record PostCreateRequest(
    //tripId는 URL에서 받음
    @NotNull
    LocalDate date,
    LocalTime time,
    @NotBlank
    @Size(max = 100)
    String title,
    String memo
) {
}
