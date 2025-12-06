package com.example.demo.service.impl;

import com.example.demo.entity.EstadoCumplimiento;
import com.example.demo.repository.EstadoCumplimientoRepository;
import com.example.demo.service.EstadoCumplimientoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadoCumplimientoServiceImpl implements EstadoCumplimientoService {

    private final EstadoCumplimientoRepository repository;

    public EstadoCumplimientoServiceImpl(EstadoCumplimientoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<EstadoCumplimiento> listar() {
        return repository.findAll();
    }

    @Override
    public EstadoCumplimiento obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
    }
}
