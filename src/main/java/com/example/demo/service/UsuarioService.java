package com.example.demo.service;

import com.example.demo.entity.Usuario;

import java.util.List;

public interface UsuarioService {
    List<Usuario> listar();
    Usuario obtenerPorId(Integer id);
    Usuario crear(Usuario usuario);
    Usuario actualizar(Integer id, Usuario usuario);
    Usuario obtenerPorCorreo(String correo);
    void eliminar(Integer id);
}
