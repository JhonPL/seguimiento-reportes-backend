-- =====================================================
-- Datos Iniciales
-- =====================================================

-- ROLES
INSERT INTO roles (id_rol, nombre, descripcion) VALUES
(1, 'Administrador', 'Control total del sistema y configuraciones'),
(2, 'Responsable', 'Usuario que elabora y envía reportes'),
(3, 'Supervisor', 'Supervisa cumplimiento de reportes'),
(4, 'Auditor', 'Consulta el estado de cumplimiento y reportes');
-- FRECUENCIAS
INSERT INTO frecuencias (nombre, dias_intervalo, descripcion) VALUES
('Mensual', 30, 'Reporte mensual'),
('Bimestral', 60, 'Reporte cada dos meses'),
('Trimestral', 90, 'Reporte trimestral'),
('Cuatrimestral', 120, 'Reporte cada cuatro meses'),
('Semestral', 180, 'Reporte semestral'),
('Anual', 365, 'Reporte anual'),
('Única Vez', NULL, 'Reporte de única presentación'),
('Específica', NULL, 'Frecuencia personalizada');

-- ESTADOS
INSERT INTO estados_cumplimiento (nombre) VALUES
('Pendiente'),
('Enviado a tiempo'),
('Enviado tarde'),
('Vencido');

-- TIPOS DE ALERTA
INSERT INTO tipos_alerta (nombre, color, dias_antes_vencimiento, es_post_vencimiento) VALUES
('Preventiva', 'Verde', 15, false),
('Seguimiento', 'Amarilla', 5, false),
('Riesgo', 'Naranja', 1, false),
('Crítica', 'Roja', NULL, true);

-- USUARIO ADMINISTRADOR
INSERT INTO usuarios (
    cedula, 
    nombre_completo, 
    correo, 
    contrasena, 
    proceso, 
    cargo, 
    telefono, 
    rol_id)   
    VALUES
    ('10000001','Javier (Admin)', 'admin@llanogas.com', 'admin123', 'TI', 'Adminsitrador', '3000000001', 1);

-- ENTIDADES DE EJEMPLO
INSERT INTO entidades (nit, razon_social, pagina_web, base_legal) VALUES
('900000000-1', 'Superintendencia de Servicios Públicos Domiciliarios', 'https://www.superservicios.gov.co', 'Ley 142 de 1994, Ley 143 de 1994'),
('800000000-2', 'Sistema Único de Información - SUI', 'https://www.sui.gov.co', 'Resolución SSPD 20051300054923'),
('700000000-3', 'Contaduría General de la Nación', 'https://www.contaduria.gov.co', 'Resolución 193 de 2016');

-- =====================================================
-- VISTAS ÚTILES
-- =====================================================

CREATE OR REPLACE VIEW v_reportes_completos AS
SELECT 
    r.id_reporte,
    r.nombre AS nombre_reporte,
    e.razon_social AS entidad,
    f.nombre AS frecuencia,
    r.dia_vencimiento,
    r.mes_vencimiento,
    ue.nombre_completo AS responsable_elaboracion,
    us.nombre_completo AS responsable_supervision,
    r.activo,
    r.fecha_inicio_vigencia,
    r.fecha_fin_vigencia
FROM reportes r
JOIN entidades e ON r.entidad_id = e.id_entidad
JOIN frecuencias f ON r.frecuencia_id = f.id_frecuencia
JOIN usuarios ue ON r.responsable_elaboracion_id = ue.id_usuario
JOIN usuarios us ON r.responsable_supervision_id = us.id_usuario;

CREATE OR REPLACE VIEW v_cumplimiento_reportes AS
SELECT 
    ir.id_instancia,
    r.id_reporte,
    r.nombre AS nombre_reporte,
    e.razon_social AS entidad,
    ir.periodo_reportado,
    ir.fecha_vencimiento_calculada,
    ir.fecha_envio_real,
    ec.nombre AS estado,
    ir.dias_desviacion,
    CASE 
        WHEN ir.dias_desviacion IS NULL THEN 'Sin enviar'
        WHEN ir.dias_desviacion <= 0 THEN 'A tiempo'
        ELSE 'Tarde'
    END AS resultado_cumplimiento,
    ue.nombre_completo AS responsable_elaboracion,
    us.nombre_completo AS responsable_supervision
FROM instancias_reporte ir
JOIN reportes r ON ir.reporte_id = r.id_reporte
JOIN entidades e ON r.entidad_id = e.id_entidad
JOIN estados_cumplimiento ec ON ir.estado_id = ec.id_estado
JOIN usuarios ue ON r.responsable_elaboracion_id = ue.id_usuario
JOIN usuarios us ON r.responsable_supervision_id = us.id_usuario;

CREATE OR REPLACE VIEW v_alertas_pendientes AS
SELECT 
    a.id_alerta,
    r.nombre AS nombre_reporte,
    ir.periodo_reportado,
    ta.nombre AS tipo_alerta,
    ta.color,
    a.fecha_programada,
    a.enviada,
    a.leida,
    u.nombre_completo AS destinatario,
    u.correo AS correo_destinatario
FROM alertas a
JOIN instancias_reporte ir ON a.instancia_reporte_id = ir.id_instancia
JOIN reportes r ON ir.reporte_id = r.id_reporte
JOIN tipos_alerta ta ON a.tipo_alerta_id = ta.id_tipo
LEFT JOIN usuarios u ON a.usuario_destino_id = u.id_usuario
WHERE a.enviada = false OR a.leida = false;

-- =====================================================
-- TRIGGERS
-- =====================================================

CREATE OR REPLACE FUNCTION actualizar_fecha_modificacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_usuarios_actualizar
    BEFORE UPDATE ON usuarios
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_modificacion();

CREATE TRIGGER tr_reportes_actualizar
    BEFORE UPDATE ON reportes
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_modificacion();

CREATE TRIGGER tr_entidades_actualizar
    BEFORE UPDATE ON entidades
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_modificacion();

CREATE TRIGGER tr_instancias_actualizar
    BEFORE UPDATE ON instancias_reporte
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_fecha_modificacion();

-- =====================================================
-- MENSAJES DE CONFIRMACIÓN
-- =====================================================
DO $$
BEGIN
    RAISE NOTICE '=================================================';
    RAISE NOTICE '✓ Base de datos inicializada correctamente';
    RAISE NOTICE '✓ Tablas creadas: %', (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE');
    RAISE NOTICE '✓ Usuario admin creado: admin@llanogas.com';
    RAISE NOTICE '✓ CAMBIAR CONTRASEÑA después del primer login';
    RAISE NOTICE '=================================================';
END $$;