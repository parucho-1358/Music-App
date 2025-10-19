package com.example.reactapt.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class PingController {

    @GetMapping(value = "/api/ping", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> ping() {
        return Mono.just(Map.of("ok", true, "ts", System.currentTimeMillis()));
    }
}
