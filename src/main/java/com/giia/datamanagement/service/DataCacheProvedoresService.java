package com.giia.datamanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giia.datamanagement.repository.ProveedorRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import org.springframework.data.redis.connection.ReactiveSubscription.Message;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DataCacheProvedoresService {

    private final ProveedorRepository proveedorRepository;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final String keyPrefix;
    private final SseService sseService;

    public DataCacheProvedoresService(ProveedorRepository proveedorRepository,
                                      ReactiveRedisTemplate<String, Object> redisTemplate,
                                      ReactiveRedisConnectionFactory connectionFactory,
                                      @Value("${datacache.redis.key-prefix.proveedor}") String keyPrefix,
                                      @Value("${datacache.redis.channel.proveedor}") String channel,
                                      SseService sseService) {
        this.proveedorRepository = proveedorRepository;
        this.redisTemplate = redisTemplate;
        ReactiveRedisMessageListenerContainer listenerContainer = new ReactiveRedisMessageListenerContainer(connectionFactory);
        ChannelTopic channelTopic = new ChannelTopic(channel);
        this.keyPrefix = keyPrefix;
        this.sseService = sseService;

        listenerContainer.receive(channelTopic)
                .map(Message::getMessage)
                .cast(String.class)
                .flatMap(event ->
                        Mono.fromCallable(() -> {
                                    ObjectMapper mapper = new ObjectMapper();
                                    return mapper.readTree(event);
                                })
                                .flatMap(node -> {
                                    log.debug("Evento leido: {}",node);
                                    return refreshAll();
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
    @PostConstruct
    public void init() {
        refreshAll().subscribe();
    }

    public Mono<Void> refreshAll() {
        return proveedorRepository.findAll()
                .flatMap(proveedor -> {
                    String redisKey = keyPrefix + proveedor.getId();
                    return redisTemplate.opsForValue().set(redisKey, proveedor);
                })
                .then()
                .doOnSuccess(v -> {
                    // Avisamos al frontend que debe refrescar
                    sseService.publish("REFRESH_PROVEEDORES");
                });
    }

    /**
     * Polling de respaldo: cada hora refresca todo
     */
    @Scheduled(fixedRate = 3600000) // 1 hora en ms
    public void scheduledRefresh() {
        refreshAll().subscribe();
    }


}
