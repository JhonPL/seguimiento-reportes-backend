package com.example.demo.service.impl;

import com.example.demo.entity.Entidad;
import com.example.demo.repository.EntidadRepository;
import com.example.demo.service.EntidadService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntidadServiceImpl implements EntidadService {

    private final EntidadRepository repository;

    public EntidadServiceImpl(EntidadRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Entidad> listar() {
        return repository.findAll();
    }

    @Override
    public Entidad obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entidad no encontrada"));
    }

    @Override
    public Entidad crear(Entidad entidad) {
        return repository.save(entidad);
    }

    @Override
    public Entidad actualizar(Integer id, Entidad entidad) {
        Entidad existente = obtenerPorId(id);

        existente.setNit(entidad.getNit());
        existente.setRazonSocial(entidad.getRazonSocial());
        existente.setPaginaWeb(entidad.getPaginaWeb());
        existente.setBaseLegal(entidad.getBaseLegal());
        existente.setActivo(entidad.isActivo());

        return repository.save(existente);
    }

    @Override
    public void eliminar(Integer id) {
        repository.deleteById(id);
    }
}
