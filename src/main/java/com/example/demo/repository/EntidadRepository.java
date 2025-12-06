package com.example.demo.repository;

import com.example.demo.entity.Entidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntidadRepository extends JpaRepository<Entidad, Integer> {
    Optional<Entidad> findByNit(String nit);
}
