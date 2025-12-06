package com.example.demo.repository;

import com.example.demo.entity.Frecuencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FrecuenciaRepository extends JpaRepository<Frecuencia, Integer> {
    Optional<Frecuencia> findByNombre(String nombre);
}
