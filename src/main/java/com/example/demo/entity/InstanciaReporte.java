package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "instancias_reporte")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanciaReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instancia")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporte_id", nullable = false)
    private Reporte reporte;

    @Column(name = "periodo_reportado", nullable = false, length = 50)
    private String periodoReportado;

    @Column(name = "fecha_vencimiento_calculada")
    private LocalDate fechaVencimientoCalculada;

    @Column(name = "fecha_envio_real")
    private LocalDateTime fechaEnvioReal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoCumplimiento estado;

    @Column(name = "dias_desviacion")
    private Integer diasDesviacion;

    @Column(name = "link_reporte_final", length = 500)
    private String linkReporteFinal;

    @Column(name = "link_evidencia_envio", length = 500)
    private String linkEvidenciaEnvio;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // Usuario que envió el reporte
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "enviado_por_id")
    private Usuario enviadoPor;

    // Nombre del archivo subido
    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    // ID del archivo en Google Drive
    @Column(name = "drive_file_id", length = 100)
    private String driveFileId;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    // ========== CAMPOS PARA CORRECCIONES ==========
    
    // Indica si tiene una corrección
    @Column(name = "tiene_correccion")
    private Boolean tieneCorreccion = false;
    
    // Link al archivo de corrección en Drive
    @Column(name = "link_correccion", length = 500)
    private String linkCorreccion;
    
    // ID del archivo de corrección en Drive
    @Column(name = "drive_file_id_correccion", length = 100)
    private String driveFileIdCorreccion;
    
    // Nombre del archivo de corrección
    @Column(name = "nombre_archivo_correccion", length = 255)
    private String nombreArchivoCorreccion;
    
    // Motivo de la corrección
    @Column(name = "motivo_correccion", columnDefinition = "TEXT")
    private String motivoCorreccion;
    
    // Fecha de la corrección
    @Column(name = "fecha_correccion")
    private LocalDateTime fechaCorreccion;
    
    // Usuario que realizó la corrección
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "corregido_por_id")
    private Usuario corregidoPor;

    // ==================================================

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}