package com.example.reactapt.config.DTO;

import java.util.List;

public record ScSearchResponse(
        int count,
        List<Item> items,
        String next
){
    public record Item(
            long id,
            String title,
            String artist,
            String artwork,
            String permalink,
            Long playbackCount,
            Long durationMs
    ){}
}
