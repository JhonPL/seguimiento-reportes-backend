package com.example.demo.repository;

import com.example.demo.entity.Alerta;
import com.example.demo.entity.InstanciaReporte;
import com.example.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Integer> {

    List<Alerta> findByInstancia(InstanciaReporte instancia);

    List<Alerta> findByUsuarioDestino(Usuario usuario);

    List<Alerta> findByUsuarioDestinoAndLeidaFalse(Usuario usuario);

    List<Alerta> findByEnviadaFalse();

@Query("SELECT COUNT(a) > 0 FROM Alerta a WHERE a.instancia = :instancia " +
       "AND a.usuarioDestino = :usuario " +
       "AND a.tipo.nombre = :tipoNombre " +
       "AND a.fechaEnviada BETWEEN :inicio AND :fin")
boolean existsByInstanciaAndUsuarioDestinoAndTipoNombreAndFechaEnviadaBetween(
        @Param("instancia") InstanciaReporte instancia,
        @Param("usuario") Usuario usuario,
        @Param("tipoNombre") String tipoNombre,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
);

}
