package com.example.demo.controller;

import com.example.demo.dto.AlertaDTO;
import com.example.demo.entity.Alerta;
import com.example.demo.service.AlertaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alertas")
@CrossOrigin
public class AlertaController {

    private final AlertaService service;

    public AlertaController(AlertaService service) {
        this.service = service;
    }

    @GetMapping
    public List<Alerta> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Alerta obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    public Alerta crear(@RequestBody Alerta alerta) {
        return service.crear(alerta);
    }

    @PutMapping("/{id}")
    public Alerta actualizar(@PathVariable Integer id, @RequestBody Alerta alerta) {
        return service.actualizar(id, alerta);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }

    @GetMapping("/instancia/{instanciaId}")
    public List<Alerta> porInstancia(@PathVariable Integer instanciaId) {
        return service.listarPorInstancia(instanciaId);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Alerta> porUsuario(@PathVariable Integer usuarioId) {
        return service.listarPorUsuario(usuarioId);
    }

    @GetMapping("/usuario/{usuarioId}/no-leidas")
    public List<Alerta> noLeidasPorUsuario(@PathVariable Integer usuarioId) {
        return service.listarNoLeidas(usuarioId);
    }

    @PatchMapping("/{id}/leer")
    public Alerta marcarComoLeida(@PathVariable Integer id) {
        return service.marcarComoLeida(id);
    }

    // ==================== NUEVOS ENDPOINTS ====================

    /**
     * Obtener mis alertas (usuario autenticado)
     */
    @GetMapping("/mis-alertas")
    public ResponseEntity<List<AlertaDTO>> misAlertas(Authentication authentication) {
        List<AlertaDTO> alertas = service.listarAlertasUsuarioActual(authentication);
        return ResponseEntity.ok(alertas);
    }

    /**
     * Obtener mis alertas no leídas (usuario autenticado)
     */
    @GetMapping("/mis-alertas/no-leidas")
    public ResponseEntity<List<AlertaDTO>> misAlertasNoLeidas(Authentication authentication) {
        List<AlertaDTO> alertas = service.listarAlertasNoLeidasUsuarioActual(authentication);
        return ResponseEntity.ok(alertas);
    }

    /**
     * Contar mis alertas no leídas
     */
    @GetMapping("/mis-alertas/contador")
    public ResponseEntity<Map<String, Object>> contadorMisAlertas(Authentication authentication) {
        long count = service.contarAlertasNoLeidasUsuarioActual(authentication);
        return ResponseEntity.ok(Map.of("noLeidas", count));
    }

    /**
     * Marcar una alerta como leída
     */
    @PatchMapping("/{id}/marcar-leida")
    public ResponseEntity<AlertaDTO> marcarLeida(@PathVariable Integer id, Authentication authentication) {
        AlertaDTO alerta = service.marcarComoLeidaDTO(id, authentication);
        return ResponseEntity.ok(alerta);
    }

    /**
     * Marcar todas mis alertas como leídas
     */
    @PatchMapping("/mis-alertas/marcar-todas-leidas")
    public ResponseEntity<Map<String, Object>> marcarTodasLeidas(Authentication authentication) {
        int cantidad = service.marcarTodasComoLeidas(authentication);
        return ResponseEntity.ok(Map.of(
            "mensaje", "Alertas marcadas como leídas",
            "cantidad", cantidad
        ));
    }

    /**
     * Obtener todas las alertas (solo admin)
     */
    @GetMapping("/todas")
    public ResponseEntity<List<AlertaDTO>> todasLasAlertas(Authentication authentication) {
        List<AlertaDTO> alertas = service.listarTodasDTO(authentication);
        return ResponseEntity.ok(alertas);
    }

    /**
     * Obtener todas las alertas no leídas (solo admin)
     */
    @GetMapping("/todas/no-leidas")
    public ResponseEntity<List<AlertaDTO>> todasNoLeidas(Authentication authentication) {
        List<AlertaDTO> alertas = service.listarTodasNoLeidasDTO(authentication);
        return ResponseEntity.ok(alertas);
    }
}