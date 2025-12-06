package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertaDTO {
    private Integer id;
    private Integer instanciaId;
    private String reporteNombre;
    private String periodoReportado;
    private Integer tipoAlertaId;
    private String tipoAlertaNombre;
    private String tipoAlertaColor;
    private Integer usuarioDestinoId;
    private String usuarioDestinoNombre;
    private LocalDateTime fechaProgramada;
    private LocalDateTime fechaEnviada;
    private boolean enviada;
    private String mensaje;
    private boolean leida;
    
    // Campos adicionales útiles
    private String entidadNombre;
    private LocalDateTime fechaVencimiento;
    private Integer diasHastaVencimiento;
}