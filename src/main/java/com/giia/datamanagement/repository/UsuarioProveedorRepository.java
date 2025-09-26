package com.giia.datamanagement.repository;

import com.giia.datamanagement.model.Usuario;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioProveedorRepository extends ReactiveCrudRepository<Usuario, Long> {
    Mono<Usuario> findByUsuario(String user);
}