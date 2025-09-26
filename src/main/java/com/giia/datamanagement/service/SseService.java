package com.giia.datamanagement.service;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class SseService {

    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    /**
     * Devuelve un Flux que el frontend puede suscribirse para recibir actualizaciones SSE
     */
    public Flux<String> stream() {
        return sink.asFlux();
    }

    /**
     * Envía un evento de notificación a todos los subscriptores
     */
    public void publish(String event) {
        sink.tryEmitNext(event);
    }
}
