package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "tipos_alerta")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoAlerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo")
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 20)
    private String color;

    @Column(name = "dias_antes_vencimiento")
    private Integer diasAntesVencimiento;

    @Column(name = "es_post_vencimiento")
    private boolean esPostVencimiento;
}
