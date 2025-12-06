package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reporte {

    @Id
    @Column(name = "id_reporte", length = 50)
    private String id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entidad_id", nullable = false)
    private Entidad entidad;

    @Column(name = "base_legal", columnDefinition = "TEXT")
    private String baseLegal;

    @Column(name = "fecha_inicio_vigencia")
    private LocalDate fechaInicioVigencia;

    @Column(name = "fecha_fin_vigencia")
    private LocalDate fechaFinVigencia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "frecuencia_id", nullable = false)
    private Frecuencia frecuencia;

    @Column(name = "dia_vencimiento")
    private Integer diaVencimiento;

    @Column(name = "mes_vencimiento")
    private Integer mesVencimiento;

    @Column(name = "plazo_adicional_dias")
    private Integer plazoAdicionalDias;

    @Column(name = "formato_requerido", length = 100)
    private String formatoRequerido;

    @Column(name = "link_instrucciones", length = 255)
    private String linkInstrucciones;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_elaboracion_id", nullable = false)
    private Usuario responsableElaboracion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_supervision_id", nullable = false)
    private Usuario responsableSupervision;

    private boolean activo = true;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

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
