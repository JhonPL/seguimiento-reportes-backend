package com.example.demo.dto;

import lombok.Data;

@Data
public class EnvioInstanciaDTO {
    private Integer instanciaId;
    private String observaciones;
    private String linkEvidenciaEnvio; // Link opcional de evidencia adicional
}
