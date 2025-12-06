package com.example.demo.controller;

import com.example.demo.dto.InstanciaReporteDTO;
import com.example.demo.entity.InstanciaReporte;
import com.example.demo.service.InstanciaReporteService;
import com.example.demo.service.GoogleDriveService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instancias")
@CrossOrigin
public class InstanciaReporteController {

    private final InstanciaReporteService service;
    private final GoogleDriveService driveService;

    public InstanciaReporteController(InstanciaReporteService service, GoogleDriveService driveService) {
        this.service = service;
        this.driveService = driveService;
    }

    @GetMapping
    public List<InstanciaReporteDTO> listar() {
        return service.listarDTO();
    }

    @GetMapping("/{id}")
    public InstanciaReporteDTO obtener(@PathVariable Integer id) {
        return service.obtenerDTOPorId(id);
    }

    @PostMapping
    public InstanciaReporte crear(@RequestBody InstanciaReporte instancia) {
        return service.crear(instancia);
    }

    @PutMapping("/{id}")
    public InstanciaReporte actualizar(@PathVariable Integer id, @RequestBody InstanciaReporte instancia) {
        return service.actualizar(id, instancia);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }

    @GetMapping("/reporte/{reporteId}")
    public List<InstanciaReporteDTO> porReporte(@PathVariable String reporteId) {
        return service.listarDTOPorReporte(reporteId);
    }

    /**
     * Endpoint para enviar un reporte con archivo adjunto.
     * Sube el archivo a Google Drive y actualiza la instancia.
     */
    @PostMapping(value = "/{id}/enviar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> enviarReporte(
            @PathVariable Integer id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "observaciones", required = false) String observaciones,
            @RequestParam(value = "linkEvidencia", required = false) String linkEvidencia,
            Authentication authentication) {
        
        try {
            InstanciaReporteDTO resultado = service.enviarReporte(id, archivo, observaciones, linkEvidencia, authentication);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al enviar reporte",
                "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * Endpoint para enviar solo con link (sin archivo).
     */
    @PostMapping("/{id}/enviar-link")
    public ResponseEntity<?> enviarReporteConLink(
            @PathVariable Integer id,
            @RequestBody Map<String, String> datos,
            Authentication authentication) {
        
        try {
            String linkReporte = datos.get("linkReporte");
            String observaciones = datos.get("observaciones");
            String linkEvidencia = datos.get("linkEvidencia");
            
            InstanciaReporteDTO resultado = service.enviarReporteConLink(id, linkReporte, observaciones, linkEvidencia, authentication);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Error al enviar reporte",
                "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * Obtener instancias pendientes (para gestión).
     */
    @GetMapping("/pendientes")
    public List<InstanciaReporteDTO> listarPendientes() {
        return service.listarPendientes();
    }

    /**
     * Obtener instancias vencidas.
     */
    @GetMapping("/vencidos")
    public List<InstanciaReporteDTO> listarVencidos() {
        return service.listarVencidos();
    }

    /**
     * Obtener histórico de reportes enviados.
     * - Admin: ve todo
     * - Supervisor: solo reportes de sus supervisados
     * - Responsable: solo sus reportes
     */
    @GetMapping("/historico")
    public List<InstanciaReporteDTO> listarHistorico(
            @RequestParam(required = false) String reporteId,
            @RequestParam(required = false) Integer entidadId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer mes,
            Authentication authentication) {
        return service.listarHistorico(reporteId, entidadId, year, mes, authentication);
    }

    /**
     * Verificar si Drive está habilitado.
     */
    @GetMapping("/drive-status")
    public Map<String, Object> driveStatus() {
        return Map.of(
            "enabled", driveService.isDriveEnabled(),
            "message", driveService.isDriveEnabled() 
                ? "Google Drive está configurado y funcionando" 
                : "Google Drive no está configurado. Los archivos se guardarán con link manual."
        );
    }

    // ==================== ENDPOINTS DE CORRECCIÓN ====================

    /**
     * Endpoint para agregar una CORRECCIÓN a un reporte ya enviado.
     * SOLO ADMINISTRADORES.
     * El archivo original NO se elimina, se mantiene para auditoría.
     */
    @PostMapping(value = "/{id}/corregir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> corregirReporte(
            @PathVariable Integer id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("motivo") String motivo,
            Authentication authentication) {
        
        try {
            InstanciaReporteDTO resultado = service.corregirReporte(id, archivo, motivo, authentication);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", "Corrección agregada exitosamente. El archivo original se mantiene para auditoría.",
                "instancia", resultado
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Error al agregar corrección",
                "mensaje", e.getMessage()
            ));
        }
    }

    /**
     * Endpoint para agregar una corrección solo con link (sin archivo).
     * SOLO ADMINISTRADORES.
     */
    @PostMapping("/{id}/corregir-link")
    public ResponseEntity<?> corregirReporteConLink(
            @PathVariable Integer id,
            @RequestBody Map<String, String> datos,
            Authentication authentication) {
        
        try {
            String linkCorreccion = datos.get("linkCorreccion");
            String motivo = datos.get("motivo");
            
            InstanciaReporteDTO resultado = service.corregirReporteConLink(id, linkCorreccion, motivo, authentication);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "mensaje", "Corrección agregada exitosamente.",
                "instancia", resultado
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "Error al agregar corrección",
                "mensaje", e.getMessage()
            ));
        }
    }
}