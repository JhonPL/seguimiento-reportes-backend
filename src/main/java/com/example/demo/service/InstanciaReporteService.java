package com.example.demo.service;

import com.example.demo.dto.InstanciaReporteDTO;
import com.example.demo.entity.InstanciaReporte;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface InstanciaReporteService {
    List<InstanciaReporte> listar();
    List<InstanciaReporteDTO> listarDTO();
    
    InstanciaReporte obtenerPorId(Integer id);
    InstanciaReporteDTO obtenerDTOPorId(Integer id);
    
    InstanciaReporte crear(InstanciaReporte instancia);
    InstanciaReporte actualizar(Integer id, InstanciaReporte instancia);
    void eliminar(Integer id);

    List<InstanciaReporte> listarPorReporte(String reporteId);
    List<InstanciaReporteDTO> listarDTOPorReporte(String reporteId);
    
    // Nuevos métodos para envío de reportes
    InstanciaReporteDTO enviarReporte(Integer id, MultipartFile archivo, String observaciones, 
                                       String linkEvidencia, Authentication authentication) throws IOException;
    
    InstanciaReporteDTO enviarReporteConLink(Integer id, String linkReporte, String observaciones, 
                                              String linkEvidencia, Authentication authentication);
    
    // Métodos de consulta
    List<InstanciaReporteDTO> listarPendientes();
    List<InstanciaReporteDTO> listarVencidos();
    List<InstanciaReporteDTO> listarHistorico(String reporteId, Integer entidadId, Integer year, Integer mes, Authentication authentication);

    /**
     * Permite subir una CORRECCIÓN a un reporte ya enviado.
     * SOLO para administradores.
     * El archivo original NO se elimina, se mantiene para auditoría.
     * 
     * @param id ID de la instancia
     * @param archivo Archivo de corrección
     * @param motivo Motivo de la corrección (obligatorio)
     * @param authentication Usuario autenticado
     * @return DTO actualizado
     */
    InstanciaReporteDTO corregirReporte(Integer id, MultipartFile archivo, String motivo, 
                                         Authentication authentication) throws IOException;
    
    /**
     * Permite agregar una corrección solo con link (sin archivo).
     * SOLO para administradores.
     */
    InstanciaReporteDTO corregirReporteConLink(Integer id, String linkCorreccion, String motivo,
                                                Authentication authentication);
}