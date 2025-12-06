-- =====================================================
-- Script de Inicialización - Base de Datos Reportes
-- =====================================================

-- Extensiones
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================================
-- TABLAS CATÁLOGO
-- =====================================================

CREATE TABLE roles (
    id_rol SMALLSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT
);

CREATE TABLE usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    cedula VARCHAR(30) NOT NULL UNIQUE,
    nombre_completo VARCHAR(200) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    proceso VARCHAR(150),
    cargo VARCHAR(150),
    telefono VARCHAR(30),
    rol_id SMALLINT NOT NULL REFERENCES roles(id_rol),
    activo BOOLEAN DEFAULT true,
    fecha_creacion TIMESTAMPTZ DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ
);

CREATE INDEX idx_usuarios_rol ON usuarios(rol_id);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);

CREATE TABLE entidades (
    id_entidad BIGSERIAL PRIMARY KEY,
    nit VARCHAR(50) NOT NULL UNIQUE,
    razon_social VARCHAR(250) NOT NULL,
    pagina_web VARCHAR(255),
    base_legal TEXT,
    activo BOOLEAN DEFAULT true,
    fecha_creacion TIMESTAMPTZ DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ
);

CREATE INDEX idx_entidades_activo ON entidades(activo);

CREATE TABLE frecuencias (
    id_frecuencia SMALLSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    dias_intervalo INTEGER,
    descripcion TEXT
);

-- =====================================================
-- TABLAS DE REPORTES
-- =====================================================

CREATE TABLE reportes (
    id_reporte VARCHAR(50) PRIMARY KEY,
    nombre VARCHAR(250) NOT NULL,
    entidad_id BIGINT NOT NULL REFERENCES entidades(id_entidad),
    base_legal TEXT,
    fecha_inicio_vigencia DATE NOT NULL,
    fecha_fin_vigencia DATE,
    frecuencia_id SMALLINT NOT NULL REFERENCES frecuencias(id_frecuencia),
    dia_vencimiento SMALLINT,
    mes_vencimiento SMALLINT,
    plazo_adicional_dias INTEGER DEFAULT 0,
    formato_requerido VARCHAR(100),
    link_instrucciones VARCHAR(1024),
    responsable_elaboracion_id BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    responsable_supervision_id BIGINT NOT NULL REFERENCES usuarios(id_usuario),
    activo BOOLEAN DEFAULT true,
    fecha_creacion TIMESTAMPTZ DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ,

    CONSTRAINT chk_dia_vencimiento CHECK (dia_vencimiento IS NULL OR dia_vencimiento BETWEEN 1 AND 31),
    CONSTRAINT chk_mes_vencimiento CHECK (mes_vencimiento IS NULL OR mes_vencimiento BETWEEN 1 AND 12),
    CONSTRAINT chk_vigencia CHECK (fecha_fin_vigencia IS NULL OR fecha_fin_vigencia > fecha_inicio_vigencia)
);

CREATE INDEX idx_reportes_entidad ON reportes(entidad_id);
CREATE INDEX idx_reportes_responsable_elab ON reportes(responsable_elaboracion_id);
CREATE INDEX idx_reportes_responsable_super ON reportes(responsable_supervision_id);
CREATE INDEX idx_reportes_activo ON reportes(activo);

CREATE TABLE reporte_notificaciones (
    id BIGSERIAL PRIMARY KEY,
    reporte_id VARCHAR(50) NOT NULL REFERENCES reportes(id_reporte) ON DELETE CASCADE,
    correo VARCHAR(150) NOT NULL
);

CREATE INDEX idx_reporte_notif ON reporte_notificaciones(reporte_id);

-- =====================================================
-- TABLAS DE CUMPLIMIENTO
-- =====================================================

CREATE TABLE estados_cumplimiento (
    id_estado SMALLSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE instancias_reporte (
    id_instancia BIGSERIAL PRIMARY KEY,
    reporte_id VARCHAR(50) NOT NULL REFERENCES reportes(id_reporte),
    periodo_reportado VARCHAR(100) NOT NULL,
    fecha_vencimiento_calculada DATE NOT NULL,
    fecha_envio_real TIMESTAMPTZ,
    estado_id SMALLINT NOT NULL REFERENCES estados_cumplimiento(id_estado),
    dias_desviacion INTEGER,
    link_reporte_final VARCHAR(1024),
    link_evidencia_envio VARCHAR(1024),
    observaciones TEXT,
    fecha_creacion TIMESTAMPTZ DEFAULT NOW(),
    fecha_actualizacion TIMESTAMPTZ,

    -- Campos añadidos posteriormente (ya integrados)
    drive_file_id VARCHAR(100),
    nombre_archivo VARCHAR(255),
    enviado_por_id BIGINT REFERENCES usuarios(id_usuario),

    tiene_correccion BOOLEAN DEFAULT false,
    link_correccion VARCHAR(500),
    drive_file_id_correccion VARCHAR(100),
    nombre_archivo_correccion VARCHAR(255),
    motivo_correccion TEXT,
    fecha_correccion TIMESTAMPTZ,
    corregido_por_id BIGINT REFERENCES usuarios(id_usuario),

    CONSTRAINT uk_instancia_reporte UNIQUE (reporte_id, periodo_reportado),
    CONSTRAINT chk_fecha_envio CHECK (fecha_envio_real IS NULL OR fecha_envio_real >= fecha_creacion)
);

CREATE INDEX idx_instancias_reporte ON instancias_reporte(reporte_id);
CREATE INDEX idx_instancias_fecha_venc ON instancias_reporte(fecha_vencimiento_calculada);
CREATE INDEX idx_instancias_estado ON instancias_reporte(estado_id);
CREATE INDEX idx_instancias_periodo ON instancias_reporte(periodo_reportado);
CREATE INDEX idx_instancias_tiene_correccion 
    ON instancias_reporte(tiene_correccion) 
    WHERE tiene_correccion = true;

-- =====================================================
-- TABLAS DE ALERTAS
-- =====================================================

CREATE TABLE tipos_alerta (
    id_tipo SMALLSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(20),
    dias_antes_vencimiento INTEGER,
    es_post_vencimiento BOOLEAN DEFAULT false
);

CREATE TABLE alertas (
    id_alerta BIGSERIAL PRIMARY KEY,
    instancia_reporte_id BIGINT NOT NULL REFERENCES instancias_reporte(id_instancia) ON DELETE CASCADE,
    tipo_alerta_id SMALLINT NOT NULL REFERENCES tipos_alerta(id_tipo),
    usuario_destino_id BIGINT REFERENCES usuarios(id_usuario),
    fecha_programada TIMESTAMPTZ NOT NULL,
    fecha_enviada TIMESTAMPTZ,
    enviada BOOLEAN DEFAULT false,
    mensaje TEXT,
    leida BOOLEAN DEFAULT false,
    fecha_creacion TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_alertas_instancia ON alertas(instancia_reporte_id);
CREATE INDEX idx_alertas_usuario ON alertas(usuario_destino_id, leida);
CREATE INDEX idx_alertas_programada ON alertas(fecha_programada, enviada);

-- =====================================================
-- TABLAS DE AUDITORÍA
-- =====================================================

CREATE TABLE historial_cambios (
    id_historial BIGSERIAL PRIMARY KEY,
    tabla VARCHAR(100) NOT NULL,
    registro_id VARCHAR(255) NOT NULL,
    usuario_id BIGINT REFERENCES usuarios(id_usuario),
    campo_modificado VARCHAR(200) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo TEXT,
    fecha_modificacion TIMESTAMPTZ DEFAULT NOW(),
    comentario TEXT
);

CREATE INDEX idx_historial_tabla_registro ON historial_cambios(tabla, registro_id);
CREATE INDEX idx_historial_usuario ON historial_cambios(usuario_id);
CREATE INDEX idx_historial_fecha ON historial_cambios(fecha_modificacion);

CREATE TABLE logs_sistema (
    id_log BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id_usuario),
    accion VARCHAR(150) NOT NULL,
    modulo VARCHAR(80),
    descripcion TEXT,
    ip VARCHAR(45),
    fecha TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_logs_usuario ON logs_sistema(usuario_id);
CREATE INDEX idx_logs_fecha ON logs_sistema(fecha);
CREATE INDEX idx_logs_modulo ON logs_sistema(modulo);

-- =====================================================
-- ENCRIPTAR CONTRASEÑAS EXISTENTES
-- =====================================================

UPDATE usuarios
SET contrasena = crypt(contrasena, gen_salt('bf'));
