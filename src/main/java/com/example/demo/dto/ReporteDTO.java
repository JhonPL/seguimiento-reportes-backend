package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReporteDTO {
    private String id;
    private String nombre;
    private Integer entidadId;
    private String entidadNombre;
    private String baseLegal;
    private LocalDate fechaInicioVigencia;
    private LocalDate fechaFinVigencia;
    private Integer frecuenciaId;
    private String frecuenciaNombre;
    private Integer diaVencimiento;
    private Integer mesVencimiento;
    private Integer plazoAdicionalDias;
    private String formatoRequerido;
    private String linkInstrucciones;
    private Integer responsableElaboracionId;
    private String responsableElaboracionNombre;
    private Integer responsableSupervisionId;
    private String responsableSupervisionNombre;
    private boolean activo;
}
