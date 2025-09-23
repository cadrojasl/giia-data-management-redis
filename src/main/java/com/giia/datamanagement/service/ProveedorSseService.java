package com.giia.datamanagement.service;

import com.giia.datamanagement.model.Proveedor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ProveedorSseService {


    private final Sinks.Many<Proveedor> sink = Sinks.many().multicast().onBackpressureBuffer();

    /**
     * Devuelve un Flux que el frontend puede suscribirse para recibir actualizaciones SSE
     */
    public Flux<Proveedor> stream() {
        return sink.asFlux();
    }

    /**
     * Envia un proveedor actualizado a todos los subscriptores
     */
    public void publish(Proveedor proveedor) {
        sink.tryEmitNext(proveedor);
    }
}
