// src/main/java/com/example/reactapt/controller/ScController.java
package com.example.reactapt.controller;

import com.example.reactapt.config.DTO.ScSearchResponse;
import com.example.reactapt.service.ScSearchService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/sc")
public class ScController {

    private final ScSearchService service;

    public ScController(ScSearchService service) {
        this.service = service;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ScSearchResponse> search(

            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @RequestParam(name = "genre", required = false, defaultValue = "all-music") String genre,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor
    ) {
        // 둘 다 비면 장르 기본값으로
        if (q.isBlank() && (genre == null || genre.isBlank())) {
            genre = "all-music";
        }
        return service.search(q, genre, limit, cursor);
    }


    // 선택: 헬스체크
    @GetMapping("/ping")
    public Object ping() {
        return java.util.Map.of("ok", true, "clientId", service.clientIdMasked());
    }
}
