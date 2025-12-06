package com.example.demo.dto;

import lombok.Data;

@Data
public class UsuarioDTO {
    private Integer id;
    private String cedula;
    private String nombreCompleto;
    private String correo;
    private String proceso;
    private String cargo;
    private String telefono;
    private String rol;
    private boolean activo;
}
