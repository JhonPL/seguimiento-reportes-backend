package com.example.demo.service;

import com.example.demo.entity.Rol;
import java.util.List;

public interface RolService {
    List<Rol> listar();
    Rol obtenerPorId(Integer id);
    Rol crear(Rol rol);
    Rol actualizar(Integer id, Rol rol);
    void eliminar(Integer id);
}
