package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "frecuencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Frecuencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_frecuencia")
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;
}
