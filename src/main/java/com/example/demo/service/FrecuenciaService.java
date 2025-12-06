package com.example.demo.service;

import com.example.demo.entity.Frecuencia;

import java.util.List;

public interface FrecuenciaService {
    List<Frecuencia> listar();
    Frecuencia obtenerPorId(Integer id);
}
