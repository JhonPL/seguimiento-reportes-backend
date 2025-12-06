package com.example.demo.service.impl;

import com.example.demo.entity.Rol;
import com.example.demo.repository.RolRepository;
import com.example.demo.service.RolService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RolServiceImpl implements RolService {

    private final RolRepository repository;

    public RolServiceImpl(RolRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Rol> listar() {
        return repository.findAll();
    }

    @Override
    public Rol obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
    }

    @Override
    public Rol crear(Rol rol) {
        return repository.save(rol);
    }

    @Override
    public Rol actualizar(Integer id, Rol rol) {
        Rol existente = obtenerPorId(id);
        existente.setNombre(rol.getNombre());
        existente.setDescripcion(rol.getDescripcion());
        return repository.save(existente);
    }

    @Override
    public void eliminar(Integer id) {
        repository.deleteById(id);
    }
}
