package com.elevenst.realtimechat.global.health;

public record HealthCheckResponse(
        String status
) {

    private static final String UP = "UP";

    public static HealthCheckResponse up() {
        return new HealthCheckResponse(UP);
    }
}
