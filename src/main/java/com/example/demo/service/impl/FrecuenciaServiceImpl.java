package com.example.demo.service.impl;

import com.example.demo.entity.Frecuencia;
import com.example.demo.repository.FrecuenciaRepository;
import com.example.demo.service.FrecuenciaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FrecuenciaServiceImpl implements FrecuenciaService {

    private final FrecuenciaRepository repository;

    public FrecuenciaServiceImpl(FrecuenciaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Frecuencia> listar() {
        return repository.findAll();
    }

    @Override
    public Frecuencia obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Frecuencia no encontrada"));
    }
}
