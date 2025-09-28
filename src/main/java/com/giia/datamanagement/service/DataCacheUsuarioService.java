package com.giia.datamanagement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giia.datamanagement.repository.ProveedorRepository;
import com.giia.datamanagement.repository.UsuarioProveedorRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DataCacheUsuarioService {

    private final ProveedorRepository proveedorRepository;
    private final UsuarioProveedorRepository usuarioProveedorRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private final String keyPrefixUsu;
    private final String keyPrefixProv;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    public DataCacheUsuarioService(ProveedorRepository proveedorRepository,UsuarioProveedorRepository usuarioProveedorRepository,
                                   ReactiveRedisTemplate<String, String> redisTemplate,
                                   @Value("${datacache.redis.key-prefix.usuario-proveedor}") String keyPrefixUsu,
                                   @Value("${datacache.redis.key-prefix.proveedor-login}") String keyPrefixProv,
                                   SseService sseService, ObjectMapper objectMapper) {
        this.proveedorRepository = proveedorRepository;
        this.redisTemplate = redisTemplate;
        this.keyPrefixUsu = keyPrefixUsu;
        this.sseService = sseService;
        this.keyPrefixProv=keyPrefixProv;
        this.usuarioProveedorRepository =usuarioProveedorRepository;
        this.objectMapper=objectMapper;

    }

    @PostConstruct
    public void init() {
        refreshAll();
        refreshAllAdmins();
    }

    /**
     * Refresca toda la tabla desde SQL Server y la reescribe en Redis
     */
    public void refreshAll() {
         proveedorRepository.findAll()
                .flatMap(proveedor -> {
                    String redisKey = keyPrefixProv + proveedor.getUsuarioProv();
                    try {
                        String json = objectMapper.writeValueAsString(proveedor);
                        return redisTemplate.opsForValue().set(redisKey, json);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error deserializando Proveedor", e);
                    }

                })
                .then()
                .doOnSuccess(v -> {
                    // Avisamos al frontend que debe refrescar
                    sseService.publish("REFRESH_LOGIN_PROVEDORES");
                })
                 .subscribe();
    }
    public void refreshAllAdmins() {
        usuarioProveedorRepository.findAll()
                .flatMap(usuario -> {
                    String redisKey = keyPrefixUsu + usuario.getUsuario();
                    try {
                        String json = objectMapper.writeValueAsString(usuario);
                        return redisTemplate.opsForValue().set(redisKey, json);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error deserializando Usuario", e);
                    }
                })
                .then()
                .doOnSuccess(v -> {
                    // Avisamos al frontend que debe refrescar
                    sseService.publish("REFRESH_LOGIN_ADMINS");
                })
                .subscribe();
    }



}
