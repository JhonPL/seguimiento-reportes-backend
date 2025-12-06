package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "reporte_notificaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reporte_id", nullable = false)
    private Reporte reporte;

    @Column(nullable = false, length = 100)
    private String correo;
}
