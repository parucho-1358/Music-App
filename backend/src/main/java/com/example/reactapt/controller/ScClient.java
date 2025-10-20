package com.example.reactapt.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class ScClient {
    private static final String BASE = "https://api-v2.soundcloud.com";

    private final WebClient http;
    private final String clientId;

    public ScClient(@Value("${soundcloud.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        System.out.println("[CONFIG] SoundCloud client_id exists? " + !this.clientId.isEmpty());
        System.out.println("[CLASS] ScClient@" + System.identityHashCode(this)
                + " from " + ScClient.class.getProtectionDomain().getCodeSource().getLocation());

        this.http = WebClient.builder()
                .baseUrl(BASE)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader("Origin", "https://soundcloud.com")
                .defaultHeader("Referer", "https://soundcloud.com/")
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                // 네트워크 가드(선택): 느린 응답 시 끊어주고 재시도는 호출부에서
                .build();
    }

    /** BASE + path (+ query) 호출 */
    public <T> T get(String path, String query, ParameterizedTypeReference<T> type) {
        // client_id 없으면 SC가 빈 배열을 주는 일이 많으므로 즉시 경고
        if (clientId.isEmpty()) {
            System.err.println("[SC WARN] client_id is empty. Check application.yml and ENV (SC_CLIENT_ID).");
        }

        var b = UriComponentsBuilder.fromHttpUrl(BASE).path(path);

        // 1) 먼저 raw query 적용 (사용자 쿼리를 보존)
        if (query != null && !query.isBlank()) b.query(query);

        // 2) 기본 파라미터 없을 때만 보강 (덮어쓰기 금지)
        var qp = b.build(true).getQueryParams();
        if (!qp.containsKey("client_id"))           b.queryParam("client_id", clientId);
        if (!qp.containsKey("linked_partitioning")) b.queryParam("linked_partitioning", "1");
        if (!qp.containsKey("app_locale"))          b.queryParam("app_locale", "en");
        if (!qp.containsKey("app_version"))         b.queryParam("app_version", "1760349581");

        var url = b.build(true).toUriString();
        System.out.println("[SC GET] " + url);

        return http.get()
                .uri(url)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class)
                                .doOnNext(body -> System.err.println("[SC BODY] " + body))
                                .then(Mono.error(new RuntimeException("SC " + resp.statusCode())))
                )
                .bodyToMono(type)
                .timeout(Duration.ofSeconds(20))
                .block();
    }

    /** next_href 절대 URL 호출 (부족한 기본 파라미터만 보강) */
    public <T> T getAbsolute(String nextHref, ParameterizedTypeReference<T> type) {
        var b  = UriComponentsBuilder.fromHttpUrl(nextHref);
        var qp = b.build(true).getQueryParams();

        if (!qp.containsKey("client_id"))           b.queryParam("client_id", clientId);
        if (!qp.containsKey("linked_partitioning")) b.queryParam("linked_partitioning", "1");
        if (!qp.containsKey("app_locale"))          b.queryParam("app_locale", "en");
        if (!qp.containsKey("app_version"))         b.queryParam("app_version", "1760349581");

        // ❗ next_href는 이미 완성된 URL일 확률이 높음 → encode 호출하지 않음
        var url = b.build(true).toUriString();

        System.out.println("[SC ABS] " + url);

        return http.get()
                .uri(url)
                .retrieve()
                .onStatus(s -> !s.is2xxSuccessful(), resp ->
                        resp.bodyToMono(String.class)
                                .doOnNext(body -> System.err.println("[SC ABS BODY] " + body))
                                .then(Mono.error(new RuntimeException("SC " + resp.statusCode())))
                )
                .bodyToMono(type)
                .timeout(Duration.ofSeconds(20))
                .block();
    }
}
