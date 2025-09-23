package com.giia.datamanagement.controller;

import com.giia.datamanagement.model.Proveedor;
import com.giia.datamanagement.service.ProveedorSseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ProveedorSseController {

    private final ProveedorSseService sseService;

    public ProveedorSseController(ProveedorSseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(value = "/stream/proveedores", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Proveedor> streamProveedores() {
        return sseService.stream();
    }
}
