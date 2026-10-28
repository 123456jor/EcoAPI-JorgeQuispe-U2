INSERT INTO rol(nombre) VALUES ('AUDITOR');
INSERT INTO privilegio(nombre) VALUES ('LECTURA_GLOBAL');
INSERT INTO rol_privilegio
SELECT r.id,p.nombre FROM rol r CROSS JOIN privilegio p
WHERE r.nombre='AUDITOR' AND p.nombre IN ('PUNTO_LEER','PESAJE_LEER','LECTURA_GLOBAL');
