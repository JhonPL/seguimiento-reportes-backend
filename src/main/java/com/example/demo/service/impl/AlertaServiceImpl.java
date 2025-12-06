package com.example.demo.service.impl;

import com.example.demo.dto.AlertaDTO;
import com.example.demo.entity.Alerta;
import com.example.demo.entity.InstanciaReporte;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.AlertaRepository;
import com.example.demo.repository.InstanciaReporteRepository;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.service.AlertaService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertaServiceImpl implements AlertaService {

    private final AlertaRepository repository;
    private final InstanciaReporteRepository instanciaRepo;
    private final UsuarioRepository usuarioRepo;

    public AlertaServiceImpl(AlertaRepository repository,
                             InstanciaReporteRepository instanciaRepo,
                             UsuarioRepository usuarioRepo) {
        this.repository = repository;
        this.instanciaRepo = instanciaRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    public List<Alerta> listar() {
        return repository.findAll();
    }

    @Override
    public Alerta obtenerPorId(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));
    }

    @Override
    public Alerta crear(Alerta alerta) {
        return repository.save(alerta);
    }

    @Override
    public Alerta actualizar(Integer id, Alerta alerta) {
        Alerta existente = obtenerPorId(id);

        existente.setInstancia(alerta.getInstancia());
        existente.setTipo(alerta.getTipo());
        existente.setUsuarioDestino(alerta.getUsuarioDestino());
        existente.setFechaProgramada(alerta.getFechaProgramada());
        existente.setFechaEnviada(alerta.getFechaEnviada());
        existente.setEnviada(alerta.isEnviada());
        existente.setMensaje(alerta.getMensaje());
        existente.setLeida(alerta.isLeida());

        return repository.save(existente);
    }

    @Override
    public void eliminar(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public List<Alerta> listarPorInstancia(Integer instanciaId) {
        InstanciaReporte instancia = instanciaRepo.findById(instanciaId)
                .orElseThrow(() -> new RuntimeException("Instancia no encontrada"));

        return repository.findByInstancia(instancia);
    }

    @Override
    public List<Alerta> listarPorUsuario(Integer usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return repository.findByUsuarioDestino(usuario);
    }

    @Override
    public List<Alerta> listarNoLeidas(Integer usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return repository.findByUsuarioDestinoAndLeidaFalse(usuario);
    }

    @Override
    public Alerta marcarComoLeida(Integer id) {
        Alerta alerta = obtenerPorId(id);
        alerta.setLeida(true);
        return repository.save(alerta);
    }

    // ==================== NUEVOS MÉTODOS ====================

    @Override
    public List<AlertaDTO> listarAlertasUsuarioActual(Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        
        // Todos los usuarios (incluido Admin) solo ven sus propias alertas
        List<Alerta> alertas = repository.findByUsuarioDestino(usuario);
        
        return alertas.stream()
                .map(this::convertirADTO)
                .sorted(Comparator.comparing(AlertaDTO::getFechaProgramada).reversed())
                .limit(50) // Máximo 50 alertas
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertaDTO> listarAlertasNoLeidasUsuarioActual(Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        
        // Todos los usuarios solo ven sus propias alertas no leídas
        List<Alerta> alertas = repository.findByUsuarioDestinoAndLeidaFalse(usuario);
        
        return alertas.stream()
                .map(this::convertirADTO)
                .sorted(Comparator.comparing(AlertaDTO::getFechaProgramada).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public long contarAlertasNoLeidasUsuarioActual(Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        return repository.findByUsuarioDestinoAndLeidaFalse(usuario).size();
    }

    @Override
    @Transactional
    public AlertaDTO marcarComoLeidaDTO(Integer id, Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        Alerta alerta = obtenerPorId(id);
        
        // Verificar que la alerta pertenece al usuario actual
        if (alerta.getUsuarioDestino() == null || !alerta.getUsuarioDestino().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para modificar esta alerta");
        }
        
        alerta.setLeida(true);
        Alerta guardada = repository.save(alerta);
        return convertirADTO(guardada);
    }

    @Override
    @Transactional
    public int marcarTodasComoLeidas(Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        
        // Solo marca como leídas las alertas del usuario actual
        List<Alerta> alertasNoLeidas = repository.findByUsuarioDestinoAndLeidaFalse(usuario);
        
        for (Alerta alerta : alertasNoLeidas) {
            alerta.setLeida(true);
            repository.save(alerta);
        }
        
        return alertasNoLeidas.size();
    }

    @Override
    public List<AlertaDTO> listarTodasDTO(Authentication authentication) {
        // Verificar que sea admin - esto es para ver TODAS las alertas del sistema (panel de administración)
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        String rol = usuario.getRol() != null ? usuario.getRol().getNombre().toUpperCase() : "";
        
        if (!rol.contains("ADMIN")) {
            throw new RuntimeException("Acceso denegado: solo administradores");
        }
        
        return repository.findAll().stream()
                .map(this::convertirADTO)
                .sorted(Comparator.comparing(AlertaDTO::getFechaProgramada).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<AlertaDTO> listarTodasNoLeidasDTO(Authentication authentication) {
        Usuario usuario = obtenerUsuarioAutenticado(authentication);
        String rol = usuario.getRol() != null ? usuario.getRol().getNombre().toUpperCase() : "";
        
        if (!rol.contains("ADMIN")) {
            throw new RuntimeException("Acceso denegado: solo administradores");
        }
        
        return repository.findAll().stream()
                .filter(a -> !a.isLeida())
                .map(this::convertirADTO)
                .sorted(Comparator.comparing(AlertaDTO::getFechaProgramada).reversed())
                .collect(Collectors.toList());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Usuario obtenerUsuarioAutenticado(Authentication authentication) {
        String correo = authentication.getName();
        return usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private AlertaDTO convertirADTO(Alerta alerta) {
        AlertaDTO dto = new AlertaDTO();
        
        dto.setId(alerta.getId());
        dto.setInstanciaId(alerta.getInstancia() != null ? alerta.getInstancia().getId() : null);
        
        // Datos del reporte
        if (alerta.getInstancia() != null && alerta.getInstancia().getReporte() != null) {
            dto.setReporteNombre(alerta.getInstancia().getReporte().getNombre());
            dto.setPeriodoReportado(alerta.getInstancia().getPeriodoReportado());
        }
        
        // Tipo de alerta
        if (alerta.getTipo() != null) {
            dto.setTipoAlertaId(alerta.getTipo().getId());
            dto.setTipoAlertaNombre(alerta.getTipo().getNombre());
            dto.setTipoAlertaColor(alerta.getTipo().getColor());
        }
        
        // Usuario destino
        if (alerta.getUsuarioDestino() != null) {
            dto.setUsuarioDestinoId(alerta.getUsuarioDestino().getId());
            dto.setUsuarioDestinoNombre(alerta.getUsuarioDestino().getNombreCompleto());
        }
        
        dto.setFechaProgramada(alerta.getFechaProgramada());
        dto.setFechaEnviada(alerta.getFechaEnviada());
        dto.setEnviada(alerta.isEnviada());
        dto.setMensaje(alerta.getMensaje());
        dto.setLeida(alerta.isLeida());
        
        return dto;
    }
}