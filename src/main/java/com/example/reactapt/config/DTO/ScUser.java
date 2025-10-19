package com.example.reactapt.config.DTO;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScUser(
        long id,
        String username,
        @JsonProperty("permalink_url") String permalinkUrl,
        @JsonProperty("avatar_url")    String avatarUrl
) {}
