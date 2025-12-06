package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InstanciaReporteDTO {
    private Integer id;
    
    // Datos del reporte
    private String reporteId;
    private String reporteNombre;
    private String entidadNombre;
    private Integer entidadId;
    private String frecuencia;
    private String formatoRequerido;
    private String baseLegal;
    
    // Datos de la instancia
    private String periodoReportado;
    private LocalDate fechaVencimientoCalculada;
    private LocalDateTime fechaEnvioReal;
    private String estadoNombre;
    private Integer estadoId;
    private Integer diasDesviacion;
    private Integer diasHastaVencimiento;
    
    // Archivos y links
    private String linkReporteFinal;
    private String linkEvidenciaEnvio;
    private String nombreArchivo;
    private String driveFileId;
    
    // Usuario
    private String responsableElaboracion;
    private Integer responsableElaboracionId;
    private String responsableSupervision;
    private Integer responsableSupervisionId;
    private String enviadoPorNombre;
    private Integer enviadoPorId;
    
    // Otros
    private String observaciones;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    
    // Estado calculado
    private String prioridad; // CRITICA, ALTA, MEDIA, BAJA
    private boolean vencido;
    private boolean enviado;
    
    // ========== CAMPOS PARA CORRECCIONES ==========
    private Boolean tieneCorreccion;
    private String linkCorreccion;
    private String driveFileIdCorreccion;
    private String nombreArchivoCorreccion;
    private String motivoCorreccion;
    private LocalDateTime fechaCorreccion;
    private String corregidoPorNombre;
    private Integer corregidoPorId;
    
    // Flag que indica si se puede corregir (solo admin y si ya fue enviado)
    private boolean puedeCorregir;
}