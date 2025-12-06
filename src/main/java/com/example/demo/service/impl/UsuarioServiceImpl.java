package com.example.demo.service.impl;

import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Usuario> listar() {
        return repository.findAll();
    }

    @Override
    public Usuario obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Override
    public Usuario crear(Usuario usuario) {
        if (repository.existsByCedula(usuario.getCedula()))
            throw new RuntimeException("La cédula ya está registrada");

        if (repository.existsByCorreo(usuario.getCorreo()))
            throw new RuntimeException("El correo ya está registrado");

        // Encriptar contraseña
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        
        return repository.save(usuario);
    }

    @Override
    public Usuario obtenerPorCorreo(String correo) {
        return repository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con correo: " + correo));
    }

    @Override
    public Usuario actualizar(Integer id, Usuario usuario) {
        Usuario existente = obtenerPorId(id);

        existente.setNombreCompleto(usuario.getNombreCompleto());
        existente.setCorreo(usuario.getCorreo());
        existente.setProceso(usuario.getProceso());
        existente.setCargo(usuario.getCargo());
        existente.setTelefono(usuario.getTelefono());
        existente.setRol(usuario.getRol());
        existente.setActivo(usuario.isActivo());

        // Solo actualizar contraseña si se proporciona una nueva
        if (usuario.getContrasena() != null && !usuario.getContrasena().isEmpty()) {
            existente.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        }

        return repository.save(existente);
    }

    @Override
    public void eliminar(Integer id) {
        repository.deleteById(id);
    }
}
