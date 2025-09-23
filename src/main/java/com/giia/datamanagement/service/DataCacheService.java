package com.giia.datamanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giia.datamanagement.model.Proveedor;
import com.giia.datamanagement.repository.ProveedorRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.connection.ReactiveSubscription.Message;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class DataCacheService {

    private final ProveedorRepository proveedorRepository;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ReactiveRedisMessageListenerContainer listenerContainer;
    private final String keyPrefix;
    private final ChannelTopic channelTopic;
    private final ProveedorSseService sseService;

    public DataCacheService(ProveedorRepository proveedorRepository,
                            ReactiveRedisTemplate<String, Object> redisTemplate,
                            ReactiveRedisConnectionFactory connectionFactory,
                            @Value("${datacache.redis.key-prefix.proveedor}") String keyPrefix,
                            @Value("${datacache.redis.channel.proveedor}") String channel,
                            ProveedorSseService sseService) {
        this.proveedorRepository = proveedorRepository;
        this.redisTemplate = redisTemplate;
        this.listenerContainer = new ReactiveRedisMessageListenerContainer(connectionFactory);
        this.channelTopic = new ChannelTopic(channel);
        this.keyPrefix = keyPrefix;
        this.sseService = sseService;


        this.listenerContainer.receive(channelTopic)
                .map(Message::getMessage)
                .cast(String.class)
                .flatMap(event ->
                        Mono.fromCallable(() -> {
                                    ObjectMapper mapper = new ObjectMapper();
                                    return mapper.readTree(event);
                                })
                                .flatMap(node -> {
                                    if ("PROVEEDOR".equals(node.path("type").asText())) {
                                        return refreshAll();
                                    }
                                    return Mono.empty();
                                })
                )
                .onErrorResume(e -> {
                    log.error("Error parseando mensaje Redis", e);
                    return Mono.empty();
                })
                .subscribe();
    }

    /**
     * Refresca toda la tabla desde SQL Server y la reescribe en Redis
     */
    public Mono<Void> refreshAll() {
        return proveedorRepository.findAll()
                .flatMap(proveedor -> {
                    String redisKey = keyPrefix + proveedor.getId();
                    return redisTemplate.opsForValue().set(redisKey, proveedor)
                            .doOnSuccess(v -> sseService.publish(proveedor)); // Notifica SSE
                })
                .then();
    }

    /**
     * Polling de respaldo: cada hora refresca todo
     */
    @Scheduled(fixedRate = 3600000) // 1 hora en ms
    public void scheduledRefresh() {
        refreshAll().subscribe();
    }

    /**
     * Obtiene un proveedor desde Redis
     */
    public Mono<Proveedor> getById(Long id) {
        String redisKey = keyPrefix + id;
        return redisTemplate.opsForValue()
                .get(redisKey)
                .cast(Proveedor.class);
    }

    /**
     * Obtiene toda la lista desde Redis
     */
    public Flux<Proveedor> getAll() {
        return redisTemplate.keys(keyPrefix + "*")
                .flatMap(key -> redisTemplate.opsForValue()
                        .get(key)
                        .cast(Proveedor.class));
    }
/*
 //solo para pruebas
    public Mono<Proveedor> insertProveedor(Proveedor proveedor) {
        Map<String, String> eventMessage = Map.of("type", "PROVEEDOR"); // objeto JSON
        return proveedorRepository.save(proveedor)
                .flatMap(saved -> redisTemplate.convertAndSend(channelTopic.getTopic(), eventMessage)
                        .thenReturn(saved));
    }

 */
}
