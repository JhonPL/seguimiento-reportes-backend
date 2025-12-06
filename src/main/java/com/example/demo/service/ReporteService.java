package com.example.demo.service;

import com.example.demo.entity.Reporte;

import java.util.List;

public interface ReporteService {
    List<Reporte> listar();
    Reporte obtenerPorId(String id);
    Reporte crear(Reporte reporte);
    Reporte actualizar(String id, Reporte reporte);
    void eliminar(String id);

    List<Reporte> listarPorEntidad(Integer entidadId);
    List<Reporte> listarPorFrecuencia(Integer frecuenciaId);
}
