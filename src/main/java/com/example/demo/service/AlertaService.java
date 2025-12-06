package com.example.demo.service;

import com.example.demo.dto.AlertaDTO;
import com.example.demo.entity.Alerta;
import org.springframework.security.core.Authentication;

import java.util.List;

public interface AlertaService {
    List<Alerta> listar();
    Alerta obtenerPorId(Integer id);
    Alerta crear(Alerta alerta);
    Alerta actualizar(Integer id, Alerta alerta);
    void eliminar(Integer id);

    List<Alerta> listarPorInstancia(Integer instanciaId);
    List<Alerta> listarPorUsuario(Integer usuarioId);
    List<Alerta> listarNoLeidas(Integer usuarioId);
    Alerta marcarComoLeida(Integer id);

    // Nuevos métodos para el frontend
    List<AlertaDTO> listarAlertasUsuarioActual(Authentication authentication);
    List<AlertaDTO> listarAlertasNoLeidasUsuarioActual(Authentication authentication);
    long contarAlertasNoLeidasUsuarioActual(Authentication authentication);
    AlertaDTO marcarComoLeidaDTO(Integer id, Authentication authentication);
    int marcarTodasComoLeidas(Authentication authentication);
    
    // Para admin
    List<AlertaDTO> listarTodasDTO(Authentication authentication);
    List<AlertaDTO> listarTodasNoLeidasDTO(Authentication authentication);
}