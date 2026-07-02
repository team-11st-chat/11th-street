package com.elevenst.realtimechat.domain.member.dto;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
        @NotNull MemberRole role
) {
}
