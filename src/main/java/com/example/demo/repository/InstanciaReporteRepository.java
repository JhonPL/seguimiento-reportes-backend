package com.example.demo.repository;

import com.example.demo.entity.InstanciaReporte;
import com.example.demo.entity.Reporte;
import com.example.demo.entity.EstadoCumplimiento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InstanciaReporteRepository extends JpaRepository<InstanciaReporte, Integer> {

    List<InstanciaReporte> findByReporte(Reporte reporte);

    List<InstanciaReporte> findByEstado(EstadoCumplimiento estado);

    List<InstanciaReporte> findByFechaVencimientoCalculadaBetween(LocalDate inicio, LocalDate fin);

    List<InstanciaReporte> findByFechaEnvioRealIsNull();

    @Query("SELECT i FROM InstanciaReporte i WHERE i.fechaVencimientoCalculada <= :fecha AND i.estado.nombre NOT IN ('Enviado a tiempo', 'Enviado tarde')")
    List<InstanciaReporte> findVencidos(@Param("fecha") LocalDate fecha);

    @Query("SELECT i FROM InstanciaReporte i WHERE i.fechaVencimientoCalculada BETWEEN :inicio AND :fin AND i.estado.nombre NOT IN ('Enviado a tiempo', 'Enviado tarde')")
    List<InstanciaReporte> findProximosAVencer(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    // ================= RESPONSABLE =================

    @Query("SELECT i FROM InstanciaReporte i WHERE i.reporte.responsableElaboracion.id = :responsableId AND i.fechaVencimientoCalculada BETWEEN :inicio AND :fin")
    List<InstanciaReporte> findByResponsableAndFechaVencimiento(@Param("responsableId") Integer responsableId, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT i FROM InstanciaReporte i WHERE i.reporte.responsableElaboracion.id = :responsableId AND i.fechaVencimientoCalculada BETWEEN :inicio AND :fin AND i.estado.nombre NOT IN ('Enviado a tiempo', 'Enviado tarde')")
    List<InstanciaReporte> findProximosPorResponsable(@Param("responsableId") Integer responsableId, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT i FROM InstanciaReporte i WHERE i.reporte.responsableElaboracion.id = :responsableId AND i.fechaVencimientoCalculada < :hoy AND i.estado.nombre NOT IN ('Enviado a tiempo', 'Enviado tarde')")
    List<InstanciaReporte> findVencidosPorResponsable(@Param("responsableId") Integer responsableId, @Param("hoy") LocalDate hoy);

    // ================= SUPERVISOR =================

    @Query("SELECT i FROM InstanciaReporte i WHERE i.reporte.responsableSupervision.id = :supervisorId AND i.fechaVencimientoCalculada BETWEEN :inicio AND :fin")
    List<InstanciaReporte> findBySupervisorAndFechaVencimiento(@Param("supervisorId") Integer supervisorId, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT i FROM InstanciaReporte i WHERE i.reporte.responsableSupervision.id = :supervisorId AND i.fechaVencimientoCalculada BETWEEN :inicio AND :fin AND i.estado.nombre NOT IN ('Enviado a tiempo', 'Enviado tarde')")
    List<InstanciaReporte> findProximosPorSupervisor(@Param("supervisorId") Integer supervisorId, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT i FROM InstanciaReporte i WHERE i.reporte.responsableSupervision.id = :supervisorId AND i.fechaVencimientoCalculada < :hoy AND i.estado.nombre NOT IN ('Enviado a tiempo', 'Enviado tarde')")
    List<InstanciaReporte> findVencidosPorSupervisor(@Param("supervisorId") Integer supervisorId, @Param("hoy") LocalDate hoy);

}