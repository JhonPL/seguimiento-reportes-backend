package com.example.demo.service;

import com.example.demo.entity.Entidad;
import java.util.List;

public interface EntidadService {
    List<Entidad> listar();
    Entidad obtenerPorId(Integer id);
    Entidad crear(Entidad entidad);
    Entidad actualizar(Integer id, Entidad entidad);
    void eliminar(Integer id);
}
