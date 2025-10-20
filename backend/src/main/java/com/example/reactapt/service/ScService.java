package com.example.reactapt.service;

// service/ScService.java

import com.example.reactapt.config.DTO.TrackDto;
import com.example.reactapt.config.DTO.ResolveResponse;
import com.github.benmanes.caffeine.cache.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ScService {
    private final WebClient v2;
    private final WebClient v1;
    private final String clientId;

    private final Cache<String, Object> cache =
            Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(1000).build();

    public ScService(WebClient scClient, WebClient scClientV1, String scClientId) {
        this.v2 = scClient;
        this.v1 = scClientV1;
        this.clientId = scClientId;
    }

    public Mono<Map<String, Object>> search(String q, int limit, int offset) {
        String key = "search:" + q + ":" + limit + ":" + offset;
        Object cached = cache.getIfPresent(key);
        if (cached != null) return Mono.just((Map<String, Object>) cached);

        return v2.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/tracks")
                        .queryParam("q", q)
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .queryParam("client_id", clientId)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(raw -> {
                    List<Map<String, Object>> col = (List<Map<String, Object>>) raw.getOrDefault("collection", List.of());
                    List<TrackDto> tracks = new ArrayList<>();
                    for (Map<String, Object> t : col) {
                        Map<String, Object> user = (Map<String, Object>) t.get("user");
                        String scId = String.valueOf(t.get("id"));
                        String title = (String) t.getOrDefault("title", "");
                        String artist = user != null ? (String) user.getOrDefault("username", "") : "";
                        Long duration = t.get("duration") instanceof Number n ? n.longValue() : null;
                        String permalink = (String) t.getOrDefault("permalink_url", null);
                        String artwork = (String) t.getOrDefault("artwork_url", null);
                        if (artwork == null && user != null) artwork = (String) user.getOrDefault("avatar_url", null);
                        tracks.add(new TrackDto(
                                UUID.randomUUID().toString(),
                                title, artist, duration, "soundcloud", scId, permalink, artwork
                        ));
                    }

                    String nextHref = (String) raw.get("next_href");
                    Integer nextOffset = null;
                    if (nextHref != null) {
                        try {
                            URI u = URI.create(nextHref);
                            String qs = u.getQuery();
                            for (String p : qs.split("&")) {
                                String[] kv = p.split("=");
                                if (kv.length == 2 && kv[0].equals("offset")) {
                                    nextOffset = Integer.valueOf(kv[1]);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    Map<String, Object> out = new HashMap<>();
                    out.put("tracks", tracks);
                    out.put("nextOffset", nextOffset);
                    out.put("rawNextHref", nextHref);
                    cache.put(key, out);
                    return out;
                });
    }

    public Mono<ResolveResponse> resolve(String permalink) {
        String key = "resolve:" + permalink;
        Object cached = cache.getIfPresent(key);
        if (cached != null) return Mono.just((ResolveResponse) cached);

        return v1.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/resolve")
                        .queryParam("url", permalink)
                        .queryParam("client_id", clientId)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .map(raw -> {
                    String kind = (String) raw.get("kind");
                    if ("track".equals(kind)) {
                        Map<String, Object> user = (Map<String, Object>) raw.get("user");
                        TrackDto track = new TrackDto(
                                UUID.randomUUID().toString(),
                                (String) raw.getOrDefault("title",""),
                                user != null ? (String) user.getOrDefault("username","") : "",
                                raw.get("duration") instanceof Number n ? n.longValue() : null,
                                "soundcloud",
                                String.valueOf(raw.get("id")),
                                (String) raw.getOrDefault("permalink_url", null),
                                (String) Optional.ofNullable(raw.get("artwork_url")).orElseGet(() ->
                                        user != null ? (String) user.getOrDefault("avatar_url", null) : null)
                        );
                        ResolveResponse resp = new ResolveResponse("track", track, null, null);
                        cache.put(key, resp);
                        return resp;
                    } else if ("playlist".equals(kind)) {
                        List<Map<String, Object>> arr = (List<Map<String, Object>>) raw.getOrDefault("tracks", List.of());
                        List<TrackDto> tracks = new ArrayList<>();
                        for (Map<String, Object> t : arr) {
                            Map<String, Object> user = (Map<String, Object>) t.get("user");
                            tracks.add(new TrackDto(
                                    UUID.randomUUID().toString(),
                                    (String) t.getOrDefault("title",""),
                                    user != null ? (String) user.getOrDefault("username","") : "",
                                    t.get("duration") instanceof Number n ? n.longValue() : null,
                                    "soundcloud",
                                    String.valueOf(t.get("id")),
                                    (String) t.getOrDefault("permalink_url", null),
                                    (String) Optional.ofNullable(t.get("artwork_url")).orElseGet(() ->
                                            user != null ? (String) user.getOrDefault("avatar_url", null) : null)
                            ));
                        }
                        ResolveResponse resp = new ResolveResponse("playlist",
                                null,
                                (String) raw.getOrDefault("title",""),
                                tracks);
                        cache.put(key, resp);
                        return resp;
                    } else {
                        // 알 수 없는 타입은 그대로 전달
                        ResolveResponse resp = new ResolveResponse(kind, null, null, List.of());
                        cache.put(key, resp);
                        return resp;
                    }
                });
    }
}

