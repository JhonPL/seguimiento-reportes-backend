package com.example.demo.service.impl;

import com.example.demo.entity.TipoAlerta;
import com.example.demo.repository.TipoAlertaRepository;
import com.example.demo.service.TipoAlertaService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TipoAlertaServiceImpl implements TipoAlertaService {

    private final TipoAlertaRepository repository;

    public TipoAlertaServiceImpl(TipoAlertaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TipoAlerta> listar() {
        return repository.findAll();
    }

    @Override
    public TipoAlerta obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de alerta no encontrado"));
    }
}
