package com.example.demo.repository;

import com.example.demo.entity.NotificacionReporte;
import com.example.demo.entity.Reporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionReporteRepository extends JpaRepository<NotificacionReporte, Integer> {

    List<NotificacionReporte> findByReporte(Reporte reporte);
}
