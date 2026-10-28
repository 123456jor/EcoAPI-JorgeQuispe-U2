CREATE TABLE rol (id BIGSERIAL PRIMARY KEY, nombre VARCHAR(40) UNIQUE NOT NULL);
CREATE TABLE privilegio (nombre VARCHAR(40) PRIMARY KEY);
CREATE TABLE rol_privilegio (rol_id BIGINT REFERENCES rol, privilegio VARCHAR(40) REFERENCES privilegio, PRIMARY KEY(rol_id, privilegio));
CREATE TABLE usuario (id BIGSERIAL PRIMARY KEY, usuario VARCHAR(80) UNIQUE NOT NULL, nombre VARCHAR(160) NOT NULL, clave VARCHAR(100) NOT NULL, rol_id BIGINT NOT NULL REFERENCES rol, activo BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE punto (id BIGSERIAL PRIMARY KEY, razon_social VARCHAR(200) NOT NULL, direccion VARCHAR(300) NOT NULL, zona VARCHAR(80) NOT NULL, material_habitual VARCHAR(100) NOT NULL, activo BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE ruta (id BIGSERIAL PRIMARY KEY, fecha DATE NOT NULL, estado VARCHAR(10) NOT NULL CHECK (estado IN ('ABIERTA','CERRADA')), etiqueta VARCHAR(100) UNIQUE, cerrada_por BIGINT REFERENCES usuario, cerrada_en TIMESTAMPTZ);
CREATE TABLE ruta_usuario (ruta_id BIGINT REFERENCES ruta, usuario_id BIGINT REFERENCES usuario, PRIMARY KEY(ruta_id,usuario_id));
CREATE TABLE ruta_punto (ruta_id BIGINT REFERENCES ruta, punto_id BIGINT REFERENCES punto, secuencia INT NOT NULL CHECK(secuencia > 0), PRIMARY KEY(ruta_id,punto_id), UNIQUE(ruta_id,secuencia));
CREATE TABLE omision (ruta_id BIGINT, punto_id BIGINT, motivo VARCHAR(500) NOT NULL, usuario_id BIGINT NOT NULL REFERENCES usuario, fecha_hora TIMESTAMPTZ NOT NULL DEFAULT now(), PRIMARY KEY(ruta_id,punto_id), FOREIGN KEY(ruta_id,punto_id) REFERENCES ruta_punto);
CREATE TABLE pesaje (id BIGSERIAL PRIMARY KEY, ruta_id BIGINT NOT NULL, punto_id BIGINT NOT NULL, fecha_hora TIMESTAMPTZ NOT NULL DEFAULT now(), material VARCHAR(100) NOT NULL, peso_bruto NUMERIC(14,3) NOT NULL, tara NUMERIC(14,3) NOT NULL CHECK(tara >= 0), peso_neto NUMERIC(14,3) NOT NULL CHECK(peso_neto > 0 AND peso_neto = peso_bruto-tara), observacion VARCHAR(500) NOT NULL DEFAULT '', usuario_id BIGINT NOT NULL REFERENCES usuario, anulado BOOLEAN NOT NULL DEFAULT FALSE, motivo_anulacion VARCHAR(500), FOREIGN KEY(ruta_id,punto_id) REFERENCES ruta_punto);
CREATE TABLE auditoria_pesaje (id BIGSERIAL PRIMARY KEY, pesaje_id BIGINT NOT NULL REFERENCES pesaje, actor_id BIGINT NOT NULL REFERENCES usuario, accion VARCHAR(20) NOT NULL, antes JSONB, despues JSONB, fecha_hora TIMESTAMPTZ NOT NULL DEFAULT now());
CREATE TABLE peticion (usuario_id BIGINT REFERENCES usuario, solicitud UUID NOT NULL, huella VARCHAR(64) NOT NULL, respuesta TEXT NOT NULL, PRIMARY KEY(usuario_id,solicitud));
CREATE INDEX idx_ruta_fecha ON ruta(fecha);
CREATE INDEX idx_pesaje_punto_fecha ON pesaje(punto_id,fecha_hora DESC);
CREATE INDEX idx_pesaje_ruta ON pesaje(ruta_id);
INSERT INTO rol(nombre) VALUES ('OPERARIO'),('SUPERVISOR'),('ADMINISTRADOR');
INSERT INTO privilegio(nombre) VALUES ('PUNTO_LEER'),('PESAJE_LEER'),('PESAJE_CREAR'),('PESAJE_EDITAR'),('PESAJE_ANULAR'),('RUTA_CERRAR'),('PUNTO_GESTIONAR');
INSERT INTO rol_privilegio SELECT id,p.nombre FROM rol CROSS JOIN privilegio p WHERE
 (rol.nombre='OPERARIO' AND p.nombre IN ('PUNTO_LEER','PESAJE_LEER','PESAJE_CREAR','PESAJE_EDITAR')) OR
 (rol.nombre='SUPERVISOR' AND p.nombre IN ('PUNTO_LEER','PESAJE_LEER','PESAJE_CREAR','PESAJE_EDITAR','PESAJE_ANULAR','RUTA_CERRAR')) OR
 (rol.nombre='ADMINISTRADOR' AND p.nombre IN ('PUNTO_LEER','PESAJE_LEER','PUNTO_GESTIONAR'));
