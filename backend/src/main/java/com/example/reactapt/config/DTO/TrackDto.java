package com.example.reactapt.config.DTO;

public record TrackDto(
        String id,          // local uuid (서버에서 생성해도 되고, 그대로 내려줘도 됨)
        String title,
        String artist,
        Long durationMs,
        String provider,    // "soundcloud"
        String externalId,  // SC track id
        String href,        // permalink
        String artwork
) {}
