package com.example.reactapt.service;

import com.example.reactapt.controller.ScClient;
import com.example.reactapt.config.DTO.ScPaging;
import com.example.reactapt.config.DTO.ScChartItem;
import com.example.reactapt.config.DTO.ScTrack;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MusicService {
    private final ScClient sc;
    public MusicService(ScClient sc) { this.sc = sc; }

    /**
     * ✅ charts 대신 search/tracks로 대체
     * - 첫 페이지: /search/tracks?q=<genre>&limit=<n>
     * - 다음 페이지: next_href(절대 URL)를 그대로 getAbsolute로 호출
     * - "undefined"/"null" 가드 + limit 클램프
     * - 빈 응답이면 nextHref를 null로 돌려 무한 스크롤 종료
     */
    // 기존 getTrending 그대로 두고, "첫 페이지: search/tracks" 만드는 부분만 교체
    public ScPaging<ScChartItem> getTrending(String genre, int limit, String cursor) {
        // 입력 가드
        if (genre == null || genre.isBlank()
                || "undefined".equalsIgnoreCase(genre) || "null".equalsIgnoreCase(genre)) {
            genre = "all-music";
        }
        if (cursor != null && ("undefined".equalsIgnoreCase(cursor) || "null".equalsIgnoreCase(cursor) || cursor.isBlank())) {
            cursor = null;
        }
        limit = Math.max(1, Math.min(50, limit));

        System.out.println("[SRV] getTrending(search) genre=" + genre + ", limit=" + limit + ", cursor=" + cursor);

        // 다음 페이지: next_href(절대 URL) 그대로
        if (cursor != null) {
            var page = sc.getAbsolute(cursor, new ParameterizedTypeReference<ScPaging<ScTrack>>() {});
            return toChartPage(page);
        }

        // ── 🔧 여기부터가 핵심 패치 ─────────────────────────────────────────
        // charts 네임스페이스 제거: "soundcloud:genres:all-music" → "all-music"
        String g = genre;
        if (g.startsWith("soundcloud:genres:")) {
            g = g.substring("soundcloud:genres:".length());
        }

        // search/tracks는 장르를 q가 아니라 filter.genre_or_tag로 거는 게 맞음
        // all-music은 사실상 전체이므로 필터 생략, 검색어(q)는 빈 문자열로
        StringBuilder sb = new StringBuilder();
        sb.append("q=").append(encode(""));               // ← q는 비우기
        sb.append("&limit=").append(limit);
        sb.append("&linked_partitioning=1");              // 페이지네이션

        if (!"all-music".equalsIgnoreCase(g) && !g.isBlank()) {
            sb.append("&filter.genre_or_tag=").append(encode(g)); // ← 장르 필터는 여기
        }

        var page = sc.get("/search/tracks", sb.toString(),
                new ParameterizedTypeReference<ScPaging<ScTrack>>() {});
        // ────────────────────────────────────────────────────────────────

        return toChartPage(page);
    }


    /** 기존 공개 검색 엔드포인트 (필요 시 유지) */
    public ScPaging<ScTrack> searchTracks(String q, int limit, String cursor) {
        if (cursor != null && !cursor.isBlank()
                && !"undefined".equalsIgnoreCase(cursor) && !"null".equalsIgnoreCase(cursor)) {
            return sc.getAbsolute(cursor, new ParameterizedTypeReference<ScPaging<ScTrack>>() {});
        }
        var query = "q=" + encode(q) + "&limit=" + limit + "&linked_partitioning=1";
        return sc.get("/search/tracks", query, new ParameterizedTypeReference<ScPaging<ScTrack>>() {});
    }

    // ─────────────────────────────────────────────────────────────

    private static String encode(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    /** ScPaging<ScTrack> → ScPaging<ScChartItem> 매핑 + 빈 응답 시 nextHref null 처리 */
    private ScPaging<ScChartItem> toChartPage(ScPaging<ScTrack> page) {
        if (page == null || page.collection() == null || page.collection().isEmpty()) {
            return new ScPaging<>(List.of(), null, page == null ? null : page.queryUrn(), 0);
        }
        var items = page.collection().stream().map(ScChartItem::new).toList();
        // 빈 응답이 아니면 nextHref 그대로 유지
        return new ScPaging<>(items, page.nextHref(), page.queryUrn(), page.totalResults());
    }
}
