package com.example.reactapt.service;

import com.example.reactapt.Mapper.GenreMapper;
import com.example.reactapt.config.DTO.ScSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

/**
 * SoundCloud 검색 서비스
 * - next_href 커서/offset 모두 지원
 * - 4xx/5xx 에러를 500로 뭉개지 않게 전달
 * - 에러 본문 서버로그 남김
 */
@Slf4j
@Service
public class ScSearchService {

    private final WebClient http;
    private final String clientId;

    public ScSearchService(
            WebClient.Builder builder,
            @Value("${sc.api.base:https://api-v2.soundcloud.com}") String baseUrl,
            // 환경변수 SC_CLIENT_ID → sc.client-id → soundcloud.client-id 순서로 시도
            @Value("${SC_CLIENT_ID:${sc.client-id:${soundcloud.client-id:}}}") String clientId
    ) {
        this.http = builder.baseUrl(baseUrl).build();
        this.clientId = clientId;
    }

    public Mono<ScSearchResponse> search(String q, String genreSlug, Integer limit, String cursor) {
        // 0) clientId 방어
        if (clientId == null || clientId.isBlank()) {
            log.error("SoundCloud client_id missing. Set SC_CLIENT_ID or soundcloud.client-id");
            return Mono.error(new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SoundCloud client_id missing"));
        }

        // 1) next_href를 cursor로 그대로 받은 경우(권장): 그대로 호출
        if (cursor != null && cursor.startsWith("http")) {
            URI u = URI.create(cursor);

            // cursor query에 client_id 있는지 체크
            boolean hasClientId = false;
            String query = u.getQuery();
            if (query != null) {
                for (String p : query.split("&")) {
                    if (p.startsWith("client_id=")) { hasClientId = true; break; }
                }
            }

            URI finalUri = hasClientId
                    ? u
                    : UriComponentsBuilder.fromUri(u)
                    .queryParam("client_id", clientId)
                    .build(true)
                    .toUri();

            return http.get()
                    .uri(finalUri)
                    .accept(MediaType.APPLICATION_JSON)
                    .exchangeToMono(this::handleScResponse);
        }


        // 2) 새 검색
        String keyword = (q == null || q.isBlank())
                ? GenreMapper.toKeyword(Objects.toString(genreSlug, "all-music"))
                : q;

        Map<String, List<String>> params = new HashMap<>();
        params.put("client_id", List.of(clientId));
        params.put("q", List.of(Objects.toString(keyword, "")));
        params.put("limit", List.of(String.valueOf(Objects.requireNonNullElse(limit, 12))));
        params.put("app_locale", List.of("en"));
        params.put("linked_partitioning", List.of("1")); // 중복 금지

        // offset 형태 커서도 허용
        if (cursor != null && !cursor.isBlank()) {
            params.put("offset", List.of(cursor));
        }

        return http.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/tracks")
                        .queryParams(new MultiValueMapAdapter<>(params))
                        .build()
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(this::handleScResponse);
    }

    /** SC 응답 공통 처리: 2xx → DTO, 4xx/5xx → 상태/본문 보존하여 예외 */
    private Mono<ScSearchResponse> handleScResponse(ClientResponse resp) {
        HttpStatusCode status = resp.statusCode();
        if (status.is2xxSuccessful()) {
            return resp.bodyToMono(Map.class).map(this::mapToDto);
        }
        return resp.bodyToMono(String.class).defaultIfEmpty("")
                .flatMap(body -> {
                    log.warn("SoundCloud error {}. body={}", status.value(), body);
                    if (status.is4xxClientError()) {
                        // 예: 400/401/404 → 그대로 프론트에 전달
                        return Mono.error(new ResponseStatusException(status, "SoundCloud 4xx: " + body));
                    }
                    // 5xx → 502(BAD_GATEWAY)로 변환하여 외부 연동 문제임을 명시
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "SoundCloud 5xx: " + body));
                });
    }

    @SuppressWarnings("unchecked")
    private ScSearchResponse mapToDto(Map<String, Object> scJson) {
        List<Map<String, Object>> collection =
                (List<Map<String, Object>>) scJson.getOrDefault("collection", List.of());
        String nextHref = (String) scJson.get("next_href");

        List<ScSearchResponse.Item> items = new ArrayList<>();
        for (Map<String, Object> t : collection) {
            Map<String, Object> user = (Map<String, Object>) t.getOrDefault("user", Map.of());
            items.add(new ScSearchResponse.Item(
                    ((Number) t.getOrDefault("id", 0)).longValue(),
                    Objects.toString(t.get("title"), ""),
                    Objects.toString(user.get("username"), ""),
                    Objects.toString(t.get("artwork_url"), null),
                    Objects.toString(t.get("permalink_url"), null),
                    (t.get("playback_count") instanceof Number) ? ((Number) t.get("playback_count")).longValue() : null,
                    (t.get("duration") instanceof Number) ? ((Number) t.get("duration")).longValue() : null
            ));
        }

        return new ScSearchResponse(items.size(), items, nextHref);
    }

    /** 선택: 상태 확인용(컨트롤러 /ping 등에서 사용) */
    public String clientIdMasked() {
        if (clientId == null) return "null";
        int n = clientId.length();
        return n <= 6 ? "******" : clientId.substring(0, 4) + "****" + clientId.substring(n - 2);
    }
}
