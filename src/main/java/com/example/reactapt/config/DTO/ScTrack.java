// ScTrack.java (그대로, @Data 제거!)
package com.example.reactapt.config.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScTrack(
        long id,
        String title,
        @JsonProperty("permalink_url") String permalinkUrl,
        @JsonProperty("artwork_url")   String artworkUrl
) {}
