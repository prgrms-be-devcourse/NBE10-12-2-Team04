package com.back.global.rsData;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;

public record RsData<T>(
    @NotNull String resultCode,
    @JsonIgnore int statusCode,
    @NotNull String msg,
    @NotNull T data
) {
    public RsData(String resultCode, String msg) {
        this(resultCode, msg, null);
    }

    public RsData(String resultCode, String msg, T data) {
        this(resultCode, Integer.parseInt(resultCode.split("-", 2)[0]), msg, data);
    }
}
