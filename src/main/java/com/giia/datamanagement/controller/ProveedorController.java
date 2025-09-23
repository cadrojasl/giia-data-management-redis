package com.giia.datamanagement.controller;

import com.giia.datamanagement.model.Proveedor;
import com.giia.datamanagement.service.DataCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/proveedores")
public class ProveedorController {

    private final DataCacheService dataCacheService;

    @Autowired
    public ProveedorController(DataCacheService dataCacheService) {
        this.dataCacheService = dataCacheService;
    }

    /**
     * Obtener todos los proveedores de la tabla/cache
     */
    @GetMapping("/all")
    public Mono<ResponseEntity<Flux<Proveedor>>> getAllProveedores() {
        return Mono.just(ResponseEntity.ok(dataCacheService.getAll()));
    }

    /**
     * Obtener un proveedor específico por su ID
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<Proveedor>> getProveedorById(@PathVariable Long id) {
        return dataCacheService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Refrescar cache desde BD
     */
    @GetMapping("/refresh")
    public Mono<ResponseEntity<String>> generateRefresh() {
        return dataCacheService.refreshAll()
                .then(Mono.just(ResponseEntity.ok("Cache refrescado exitosamente")));
    }

    /**
     * Insertar un nuevo proveedor en BD y disparar evento
     */
    @PostMapping("/save")
    public Mono<ResponseEntity<Proveedor>> createProveedor(@RequestBody Proveedor proveedor) {
        return dataCacheService.insertProveedor(proveedor)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }
}
