package com.rahul.urlshortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUrlRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        String url,

        @Min(value = 1, message = "expiryDays must be at least 1")
        @Max(value = 365, message = "expiryDays must be at most 365")
        Integer expiryDays
) {
}
