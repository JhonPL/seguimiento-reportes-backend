package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "estados_cumplimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCumplimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;
}
