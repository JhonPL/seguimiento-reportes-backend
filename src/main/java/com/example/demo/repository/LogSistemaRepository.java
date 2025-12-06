package com.example.demo.repository;

import com.example.demo.entity.LogSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogSistemaRepository extends JpaRepository<LogSistema, Integer> {
}
