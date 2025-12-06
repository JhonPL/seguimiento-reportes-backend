package com.example.demo.service;

import com.example.demo.entity.EstadoCumplimiento;

import java.util.List;

public interface EstadoCumplimientoService {
    List<EstadoCumplimiento> listar();
    EstadoCumplimiento obtenerPorId(Integer id);
}
