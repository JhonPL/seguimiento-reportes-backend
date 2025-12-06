package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EventoCalendarioDTO {
    private Integer id;
    private String titulo;
    private LocalDate fecha;
    private String start; // Para compatibilidad con FullCalendar
    private String end;   // Para compatibilidad con FullCalendar
    private String estado;
    private String entidad;
    private String responsable;
    private String frecuencia;
    private String periodoReportado;
    private Integer diasHastaVencimiento;
    
    // Colores para el calendario
    private String color;
    private String backgroundColor;
    private String borderColor;
    private String textColor;
    
    // Prioridad del evento
    private String prioridad; // CRITICA, ALTA, MEDIA, BAJA
    
    // Información adicional para el popup
    private String baseLegal;
    private String formatoRequerido;
    private String linkInstrucciones;
    private String observaciones;
    
    // IDs para referencias
    private String reporteId;
    private Integer entidadId;
    private Integer responsableElaboracionId;
    private Integer responsableSupervisionId;
}
