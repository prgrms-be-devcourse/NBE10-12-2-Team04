package com.triptrace.domain.post.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PostCreateRequest(
    //tripId는 URL에서 받음
    @NotNull
    LocalDate date,
    @NotBlank
    @Size(max = 100)
    String title,
    String memo
) {
}
