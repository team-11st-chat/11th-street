package com.elevenst.realtimechat.domain.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberCreateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 50) String name
) {

    @Override
    public String toString() {
        return "MemberCreateRequest{email='" + email + "', name='" + name + "', password=****}";
    }
}
