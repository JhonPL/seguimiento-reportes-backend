package com.example.demo.repository;

import com.example.demo.entity.HistorialCambios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialCambiosRepository extends JpaRepository<HistorialCambios, Integer> {

    List<HistorialCambios> findByTablaAndRegistroId(String tabla, String registroId);
}
