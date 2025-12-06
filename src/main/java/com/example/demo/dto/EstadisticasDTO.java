package com.example.demo.dto;

import lombok.Data;
import java.util.Map;

@Data
public class EstadisticasDTO {
    // Métricas principales
    private Long totalObligaciones;
    private Long totalEnviadosATiempo;
    private Long totalEnviadosTarde;
    private Long totalVencidos;
    private Long totalPendientes;
    
    // KPIs
    private Double porcentajeCumplimientoATiempo;
    private Double diasRetrasoPromedio;
    
    // Identificación de problemas
    private String entidadMayorIncumplimiento;
    private Long incumplimientosEntidadProblema;
    private String responsableMayorIncumplimiento;
    private Long incumplimientosResponsableProblema;
    
    // Distribución por estado
    private Map<String, Long> distribucionEstados;
    
    // Alertas críticas
    private Long alertasCriticasActivas;
    private Long reportesProximosVencer7Dias;
    private Long reportesProximosVencer3Dias;
    
    // Tendencia
    private Map<String, Double> tendenciaMensual;
}
