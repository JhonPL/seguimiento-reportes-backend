package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "instancia_reporte_id", nullable = false)
    private InstanciaReporte instancia;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tipo_alerta_id", nullable = false)
    private TipoAlerta tipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_destino_id", nullable = false)
    private Usuario usuarioDestino;

    @Column(name = "fecha_programada")
    private LocalDateTime fechaProgramada;

    @Column(name = "fecha_enviada")
    private LocalDateTime fechaEnviada;

    private boolean enviada;

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    private boolean leida;
}