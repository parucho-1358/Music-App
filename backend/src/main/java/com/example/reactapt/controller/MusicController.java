// src/main/java/com/example/reactapt/controller/MusicController.java
package com.example.reactapt.controller;

import com.example.reactapt.config.DTO.ScChartItem;
import com.example.reactapt.config.DTO.ScPaging;
import com.example.reactapt.config.DTO.ScTrack;
import com.example.reactapt.service.MusicService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MusicController {

    private final MusicService svc;
    public MusicController(MusicService svc) { this.svc = svc; }

    // 임시 엔드포인트
//    @GetMapping(value = "/charts/trending", produces = "application/json")
//    public Mono<ResponseEntity<String>> trendingDummy(
//            @RequestParam(defaultValue = "all-music") String genre,
//            @RequestParam(defaultValue = "20") int limit,
//            @RequestParam(required = false) String cursor
//    ) {
//        System.out.println("[CTRL] /charts/trending DUMMY HIT genre=" + genre + ", limit=" + limit + ", cursor=" + cursor);
//        String body = """
//    { "collection":[{ "track": { "id": 1, "title": "dummy-track" } }], "next_href": null }
//  """;
//        return Mono.just(ResponseEntity.ok(body));
//    }


    //  서비스로 되돌릴 "진짜" 엔드포인트

    @GetMapping("/charts/trending")
    public ScPaging<ScChartItem> trendingReal(
            @RequestParam(defaultValue = "") String genre,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        // ── 1) 입력 가드 ─────────────────────────────────────────
        // 빈/누락/undefined/null 방어
        if (genre == null || genre.isBlank()) genre = "all-music";
        if ("undefined".equalsIgnoreCase(genre) || "null".equalsIgnoreCase(genre)) genre = "all-music";

        if ("undefined".equalsIgnoreCase(cursor) || "null".equalsIgnoreCase(cursor)) cursor = null;

        // limit 클램프 (SoundCloud는 너무 큰 값 싫어함)
        limit = Math.max(1, Math.min(50, limit));

        // SoundCloud 장르 prefix 보정 (두 번 붙지 않도록)
        if (!genre.startsWith("soundcloud:genres:")) {
            genre = "soundcloud:genres:" + genre;
        }

        System.out.println("[CTRL] /charts/trending REAL genre=" + genre + ", limit=" + limit + ", cursor=" + cursor);

        try {
            // ── 2) 서비스 호출 ────────────────────────────────────
            var r = svc.getTrending(genre, limit, cursor);

            int size = (r == null || r.collection() == null) ? 0 : r.collection().size();
            System.out.println("[CTRL] REAL OK: collection=" + size);
            return (r != null) ? r : emptyPage(); // null 방지
        }
        // ── 3) 친절한 예외 처리 ─────────────────────────────────
        catch (WebClientResponseException.BadRequest e) {
            // SC 파라미터 문제 → 502 성격. 컨트롤러 시그니처가 바디만 반환이라 빈 페이지로 회복
            System.out.println("[CTRL] REAL ERR 400(BadRequest) from SC: " + e.getResponseBodyAsString());
            return emptyPage();
        } catch (WebClientResponseException.NotFound e) {
            // 다음 페이지 없을 때도 종종 옴 → 빈 페이지 반환
            System.out.println("[CTRL] REAL ERR 404(NotFound) from SC: " + e.getResponseBodyAsString());
            return emptyPage();
        } catch (WebClientResponseException e) {
            // 기타 SC 에러 → 메시지 로깅 후 빈 페이지
            System.out.println("[CTRL] REAL ERR from SC: " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
            return emptyPage();
        } catch (Exception e) {
            // 예기치 않은 에러 → 스택트레이스와 함께 빈 페이지
            e.printStackTrace();
            return emptyPage();
        }
    }

    //SoundCloud 빈 결과를 표준화해서 돌려주는 헬퍼
    private ScPaging<ScChartItem> emptyPage() {
        // ⚠️ 프로젝트의 ScPaging/ScChartItem 정의에 맞게 아래를 조정하세요.
        // 보편적인 레코드 형태: ScPaging(collection, next_href, query_urn, total_results)
        return new ScPaging<>(
                List.of(),   // collection
                null,        // next_href
                null,        // query_urn
                0            // total_results
        );
    }



    @GetMapping("/search")
    public ScPaging<ScTrack> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        return svc.searchTracks(q, limit, cursor);
    }

    @GetMapping("/ping")
    public String ping() {
        System.out.println("[CTRL] ping");
        return "ok";
    }
}
