// src/main/java/com/example/reactapt/controller/TestController.java
package com.example.reactapt.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping
    public Map<String, Object> get(@RequestParam(required=false) String message) {
        return Map.of("ok", true, "from", "backend", "receivedMessage", message);
    }

    @PostMapping
    public Map<String, Object> post(@RequestBody Map<String, Object> body) {
        return Map.of("ok", true, "from", "backend", "receivedBody", body);
    }
}
