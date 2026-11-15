package com.backend.dto;

public record TopicResponse(
        String nombre,
        int partitions,
        int replicationFactor) {
}
