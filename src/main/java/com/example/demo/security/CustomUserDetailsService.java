package com.example.demo.security;

import com.example.demo.entity.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        if (!u.isActivo()) {
            throw new UsernameNotFoundException("Usuario inactivo: " + correo);
        }

        String roleName = u.getRol() != null ? u.getRol().getNombre() : "USER";
        String role = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

        return new User(u.getCorreo(), u.getContrasena(), 
                Collections.singletonList(new SimpleGrantedAuthority(role)));
    }
}
