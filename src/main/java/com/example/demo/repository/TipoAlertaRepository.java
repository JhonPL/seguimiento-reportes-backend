package com.example.demo.repository;

import com.example.demo.entity.TipoAlerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoAlertaRepository extends JpaRepository<TipoAlerta, Integer> {
    Optional<TipoAlerta> findByNombre(String nombre);
}
