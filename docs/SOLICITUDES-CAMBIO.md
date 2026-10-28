# Parte II — solicitudes del docente

Ramas independientes desde main:

- `sc-b/auditor-lectura`: migración V2 agrega AUDITOR y privilegios de lectura. LECTURA_GLOBAL permite consultar rutas y pesajes sin asignación. No hay condicionales por nombre del rol en filtros ni controladores. La semilla crea auditor1 con SEED_PASSWORD. Las pruebas comprueban consultas y rechazos HTTP 403 en escrituras.
- `sc-c/humedad-estimada`: migración V3 agrega humedad_estimada (0–100 %, dos decimales); request, dominio, repositorio, altas, correcciones y semilla incorporan el campo. Valor predeterminado 0 para compatibilidad con clientes y registros previos. No altera el cálculo del neto.

SC-A y SC-D se implementan exclusivamente en las ramas homónimas del repositorio EcoRutas, porque corresponden a consultas y presentación de la base local.

`cambios/integracion-sc-a-b-c-d` reúne las modificaciones del servicio y corresponde a la integración Android. main y v1.0-unidad2 conservan la entrega inicial. Las migraciones no requieren borrar volúmenes ni tablas.

Validación integrada: 12 pruebas de integración aprobadas sobre PostgreSQL; incluidas lectura global del auditor, denegación de escrituras, humedad fuera de rango y persistencia en alta/corrección.
