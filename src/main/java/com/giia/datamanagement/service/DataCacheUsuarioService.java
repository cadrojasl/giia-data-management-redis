package com.giia.datamanagement.service;

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
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private final String keyPrefixUsu;
    private final String keyPrefixProv;
    private final SseService sseService;

    public DataCacheUsuarioService(ProveedorRepository proveedorRepository,UsuarioProveedorRepository usuarioProveedorRepository,
                                   ReactiveRedisTemplate<String, Object> redisTemplate,
                                   @Value("${datacache.redis.key-prefix.usuario-proveedor}") String keyPrefixUsu,
                                   @Value("${datacache.redis.key-prefix.proveedor}") String keyPrefixProv,
                                   SseService sseService) {
        this.proveedorRepository = proveedorRepository;
        this.redisTemplate = redisTemplate;
        this.keyPrefixUsu = keyPrefixUsu;
        this.sseService = sseService;
        this.keyPrefixProv=keyPrefixProv;
        this.usuarioProveedorRepository =usuarioProveedorRepository;

    }

    @PostConstruct
    public void init() {
        refreshAll();
    }

    /**
     * Refresca toda la tabla desde SQL Server y la reescribe en Redis
     */
    public void refreshAll() {
         proveedorRepository.findAll()
                .flatMap(proveedor -> {
                    String redisKey = keyPrefixProv + proveedor.getUsuarioProv();
                    return redisTemplate.opsForValue().set(redisKey, proveedor);
                })
                .then()
                .doOnSuccess(v -> {
                    // Avisamos al frontend que debe refrescar
                    sseService.publish("REFRESH_LOGIN_PROVEDORES");
                });
    }
    public void refreshAllAdmins() {
        usuarioProveedorRepository.findAll()
                .flatMap(usuario -> {
                    String redisKey = keyPrefixProv + usuario.getUsuario();
                    return redisTemplate.opsForValue().set(redisKey, usuario);
                })
                .then()
                .doOnSuccess(v -> {
                    // Avisamos al frontend que debe refrescar
                    sseService.publish("REFRESH_LOGIN_ADMINS");
                });
    }



}
