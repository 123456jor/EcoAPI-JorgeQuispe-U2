# EcoAPI — EcoRecicla

Autor: **Jorge Quispe**. Examen práctico Unidad 2, Versión B.

Servicio REST para planificar recojos de materiales reciclables, registrar pesajes y controlar el cierre de rutas. La seguridad y las reglas viven en el backend.

## Tecnologías

Kotlin 2.4.10, JVM 17, Spring Boot 3.5.7, Spring Security, Spring JDBC, PostgreSQL 17, Flyway, JWT HS256/Nimbus, BCrypt, Gradle 9.1, Docker Compose y pruebas Spring/JUnit. Se usa JDBC como acceso relacional explícito y transaccional, sin repositorios en memoria de producción.

## Arquitectura y paquetes

`src/main/kotlin/pe/upeu/ecorecicla`: `config` (JWT, seguridad, errores, semilla), `seguridad` (usuarios/roles/privilegios/login), `dominio` (entidades), `repositorio` (SQL parametrizado), `servicio` (transacciones y RN), `web/dto` (contratos) y controladores `web`. Migraciones: `src/main/resources/db/migration`.

## Entorno e instalación

Requisitos: Docker Desktop en modo Linux y Docker Compose. Para pruebas locales de Gradle, JDK 25 o un JDK compatible con Gradle 9.1; el bytecode generado apunta a Java 17. Para Newman, Node.js y npm.

Clona este repositorio y ejecuta una sola vez en PowerShell:

```powershell
./scripts/configurar.ps1
```

El script genera DATABASE_PASSWORD y JWT_SECRET aleatorios en `.env`, excluido de Git, y pide la contraseña de la semilla. Para la demostración del examen usa la clave indicada en su anexo. También puedes completar `.env.example` y guardarlo como `.env`.

## Ejecución con un comando

```powershell
docker compose up --build
```

API Windows/Postman: `http://localhost:8080`. Android Emulator: `http://10.0.2.2:8080`. La base queda en un volumen persistente; reiniciar contenedores conserva los datos. No elimines el volumen si deseas conservar la demostración. El healthcheck usa el puerto del servicio para respetar que solo login sea público.

Variables: DATABASE_PASSWORD (clave PostgreSQL), JWT_SECRET (al menos 32 bytes, se generan 64 caracteres o más), SEED_ENABLED (true solo en demostración), SEED_PASSWORD (clave de cuentas de prueba), DATABASE_URL y DATABASE_USER (Compose los configura). No hay secretos publicados.

## Datos semilla

Se crean `operario1`, `supervisor1` y `admin`, uno por rol, con BCrypt. La clave se obtiene de SEED_PASSWORD. Se cargan 12 empresas en Norte/Centro/Sur, ruta abierta actual con seis puntos, ruta anterior cerrada y 15 pesajes: uno anulado, uno de otro usuario y uno antiguo dentro de la ruta abierta para RN-02. Tres puntos de la ruta actual quedan incompletos. Las etiquetas por fecha evitan duplicados al reiniciar. En días posteriores se preparan las rutas demo de las fechas correspondientes sin borrar datos anteriores.

## API REST

Todas las rutas excepto login requieren `Authorization: Bearer <token>`. No se renuevan tokens. El login firma un token de 3600 segundos con sub, usuario, privilegios, iat, exp e issuer EcoAPI. El filtro comprueba firma, issuer, vencimiento y usuario activo.

| Método /api/v1 | Entrada | Salida / éxito | Privilegio |
|---|---|---|---|
| POST /auth/login | usuario, contrasena | token, usuario, nombre, rol, privilegios, expiraEn / 200 | Público |
| GET /usuarios/me | Bearer | id, usuario, nombre, rol, privilegios / 200 | Autenticado |
| GET /rutas/hoy?fecha=AAAA-MM-DD | fecha opcional, hoy en Lima | id, fecha, estado, puntos ordenados / 200 | PUNTO_LEER |
| GET /puntos?pagina=0&tamanio=20&zona=Norte | página, tamaño 1..100, zona opcional | Pagina<Punto> / 200 | PUNTO_LEER |
| GET /puntos/{id}/pesajes?pagina=0&tamanio=20 | punto, página, tamaño | Pagina<Pesaje>, más recientes primero / 200 | PESAJE_LEER |
| POST /pesajes | solicitudId UUID, rutaId, puntoId, material, pesoBruto, tara, observacion | Pesaje e ID definitivo, Location / 201 | PESAJE_CREAR |
| PUT /pesajes/{id} | mismos campos de escritura, nueva solicitudId | Pesaje corregido / 200 | PESAJE_EDITAR |
| DELETE /pesajes/{id}?motivo=... | motivo obligatorio | sin cuerpo / 204 | PESAJE_ANULAR |
| PUT /rutas/{ruta}/puntos/{punto}/omision | motivo | sin cuerpo / 204 | PESAJE_CREAR y acceso a ruta |
| POST /rutas/{id}/cerrar | sin cuerpo | sin cuerpo / 204 | RUTA_CERRAR |
| POST /puntos | razonSocial, direccion, zona, materialHabitual | Punto, Location / 201 | PUNTO_GESTIONAR |
| PUT /puntos/{id} | mismos campos | Punto / 200 | PUNTO_GESTIONAR |
| POST /rutas | fecha, usuarios: [Long], puntos: [Long] en orden | Ruta, Location / 201 | PUNTO_GESTIONAR |

## Reglas de negocio

RN-01: una ruta cerrada rechaza altas/correcciones con 409. RN-02: el operario corrige solo registros propios del mismo día en America/Lima; el supervisor dispone de edición global mediante privilegios. RN-03: EcoAPI calcula neto = bruto - tara y exige neto > 0, con 400 por campo. RN-04: el cierre exige pesaje no anulado o motivo de omisión en cada punto; 409 indica cuántos faltan. La anulación es lógica y conserva motivo y auditoría.

## Roles y privilegios

| Privilegio | OPERARIO | SUPERVISOR | ADMINISTRADOR |
|---|---|---|---|
| PUNTO_LEER | Sí | Sí | Sí |
| PESAJE_LEER | Sí | Sí | Sí |
| PESAJE_CREAR | Sí | Sí | No |
| PESAJE_EDITAR | Propios del día | Todos | No |
| PESAJE_ANULAR | No | Sí | No |
| RUTA_CERRAR | No | Sí | No |
| PUNTO_GESTIONAR | No | No | Sí |

Las autorizaciones se comprueban en servicios mediante @PreAuthorize y pertenencia a ruta. Los controladores no comparan nombres de rol. Los permisos efectivos se leen de la base y se intersectan con los claims firmados del token.

## Contrato común

Paginación: `contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, `ultima`. Una ruta es un agregado con sus puntos ordenados, no un listado paginado independiente.

Errores: `timestamp`, `status`, `error`, `message`, `path`, `validationErrors`. 400 indica validación; 401 token ausente/alterado/vencido; 403 privilegio o propiedad insuficiente; 404 recurso inexistente; 409 conflicto de negocio; 500 error interno. 204 nunca contiene JSON.

## Git y trazabilidad

main recibe fusiones desde develop. Cada funcionalidad se implementa en feature/* y se integra mediante PR con merge commit; las correcciones usan fix/*. Autor de todos los commits: Jorge Quispe <joorge0148@gmail.com>. El historial comenzó realmente el 28/10/2026. No acredita jornadas anteriores: no se alteraron fechas ni se fabricaron commits. Los PR remotos conservan las ramas.

La Parte II todavía no está asignada por el docente. No se creó ninguna rama sc-* ni se simularon sus tres commits durante el bloque de 80 minutos. El tag de producto identifica la versión base verificada; si la evaluación incorpora un cambio posterior, se deberá acordar explícitamente la etiqueta correspondiente sin mover un tag publicado silenciosamente.


## Persistencia e idempotencia

PostgreSQL aplica claves foráneas, índices, restricciones de peso y unicidad de secuencia. Los cambios se coordinan con @Transactional y bloqueo de fila de ruta para impedir carreras entre pesaje y cierre. `peticion(usuario_id, solicitud)` guarda la huella y la respuesta aceptada: repetir la misma solicitud no inserta otra fila; reutilizarla con otro cuerpo devuelve 409. `auditoria_pesaje` conserva actor, instante y antes/después. Flyway mantiene el esquema reproducible.

## Sincronización offline-first

EcoRutas encola operaciones en SQLDelight con UUID estable. Primero envía; después descarga. Si se pierde una respuesta, repite la misma solicitud y recupera el ID ya aceptado. EcoAPI nunca confía en un peso neto enviado por el cliente: rechaza propiedades de escritura desconocidas.

## Pruebas de integración

Con PostgreSQL iniciado, exporta las variables de `.env` al proceso y ejecuta:

```powershell
Get-Content .env | ForEach-Object { $kv=$_.Split('=',2); [Environment]::SetEnvironmentVariable($kv[0],$kv[1],'Process') }
./gradlew.bat test
```

Las diez pruebas de `EcoApiTest` usan PostgreSQL y transacciones que se revierten. Reporte: `build/reports/tests/test/index.html`. Cubren expiración y firma JWT, matriz de privilegios, las cuatro RN, paginación, aislamiento, auditoría e idempotencia.

## Postman y ejemplos

Importa `postman/EcoAPI_JorgeQuispe_Unidad2.postman_collection.json` y `EcoAPI_Local.postman_environment.json` en Postman. Configura la variable local claveExamen con la clave del anexo; deja los tokens vacíos, los login los obtienen. Ejecuta en orden. La colección crea una ruta aislada en 2099-01-01 para no cerrar la ruta actual de la app. Los IDs de usuarios y puntos se obtienen de respuestas reales.

Para repetir la validación automatizada con el ejecutor Postman Newman:

```powershell
npm ci
npm run test:api
```

El script lee `.env`, genera en memoria un JWT correctamente firmado pero vencido para la prueba y exporta un informe sin tokens. La colección se vuelve a exportar desde el SDK usado por Newman y se vacían variables sensibles. Resultado real: 31 solicitudes y 43 aserciones, cero fallos. La prueba de token vencido en Postman requiere ese token local generado por el ejecutor; el environment público lo deja vacío.

## Decisiones y limitaciones

Se eligió Spring JDBC para mantener visibles SQL, límites transaccionales y bloqueos. La creación/edición de puntos y asignación de rutas son operaciones administrativas disponibles en la API. La administración completa de cuentas/roles no forma parte de RF-B01 a RF-B07: sus asignaciones iniciales se definen en migraciones y semilla. La seguridad del entorno público requiere HTTPS, SEED_ENABLED=false y gestión externa de secretos; el Compose entregado es de demostración local.

La colección se verificó con Newman. El selector nativo de archivos de Postman no aceptó acciones del control de ventanas; no se afirma haber ejecutado su runner gráfico. Parte II y acreditación histórica de varias jornadas siguen pendientes por las razones indicadas arriba.
