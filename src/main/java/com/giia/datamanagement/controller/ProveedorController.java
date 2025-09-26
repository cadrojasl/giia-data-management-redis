package com.giia.datamanagement.controller;

import com.giia.datamanagement.service.DataCacheProvedoresService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/proveedores")
public class ProveedorController {

    private final DataCacheProvedoresService dataCacheService;


    @Autowired
    public ProveedorController(DataCacheProvedoresService dataCacheService) {
        this.dataCacheService = dataCacheService;
    }


    /**
     * Refrescar cache desde BD
     */
    @GetMapping("/refresh")
    public Mono<ResponseEntity<String>> generateRefresh() {
        return dataCacheService.refreshAll()
                .then(Mono.just(ResponseEntity.ok("Cache refrescado exitosamente")));
    }





}
