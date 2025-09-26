package com.giia.datamanagement.controller;

import com.giia.datamanagement.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class SseController {

    private final SseService sseService;

    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(value = "/stream/proveedores", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamProveedores() {
        return sseService.stream();
    }
}
