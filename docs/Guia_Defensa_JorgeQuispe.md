# Guía de defensa — Jorge Quispe

Código real de EcoRecicla. Raíces: H:\AndroidStudio\EcoAPI-JorgeQuispe-U2 y H:\AndroidStudio\EcoRutas-JorgeQuispe-U2. Los archivos Android sin prefijo de módulo están en shared/src/commonMain/kotlin/pe/upeu/ecorecicla; los del backend están en src/main/kotlin/pe/upeu/ecorecicla.

## 1. ¿Dónde se verifica el privilegio?

EcoAPI: servicio/EcoService.kt, métodos crear, editar, anular y cerrar usan @PreAuthorize. SecurityConfig habilita seguridad de métodos. El filtro autentica y el servicio autoriza. Ocultar un botón no impide enviar HTTP directamente.

Demostración y resultado esperado: Ejecutar en Newman/Postman Administrador crea pesaje u Operario intenta anular: 403. Revisar EcoApiTest.matriz de permisos se impone en el servicio.

## 2. ¿Qué transporta y quién firma el JWT?

EcoAPI: config/JwtService.kt, emitir añade sub, usuario, privilegios, iat, exp e issuer EcoAPI y firma HS256 con JWT_SECRET externo a Git. validar comprueba firma/issuer/exp; dura 3600 segundos y no tiene refresh. JwtAuthenticationFilter crea Identidad usando el usuario activo y la intersección de permisos actuales con los firmados.

Demostración y resultado esperado: Ejecutar login y las pruebas de token ausente, alterado y vencido. EcoApiTest construye un token vencido con firma válida y obtiene 401.

## 3. ¿Dónde se guarda el token?

EcoRutas: androidApp/AlmacenSeguroAndroid.kt, guardar cifra la sesión con AES-GCM. La clave se crea en AndroidKeyStore y no se exporta; SharedPreferences almacena solo IV y ciphertext. Preferencias sin cifrar exponen una credencial utilizable si se accede al archivo.

Demostración y resultado esperado: Cerrar y reabrir la aplicación conserva sesión. Cerrar sesión deja las preferencias vacías; la evidencia logout-verificacion.txt también comprueba las tablas en cero.

## 4. ¿Dónde se añade Authorization?

EcoRutas: data/remote/ClienteHttp.kt, crearCliente usa defaultRequest y bearerAuth con el token de GestorSesion. redModule proporciona un único HttpClient inyectado. Repetirlo por operación facilita omisiones y políticas inconsistentes.

Demostración y resultado esperado: EcoOfflineTest.http204NoIntentaDeserializarYCabeceraEsCentral comprueba Bearer abc en MockEngine y que 204 no se deserializa.

## 5. ¿Cómo difieren 401 y 403?

data/remote/MapeoErrores.kt crea NoAutorizado o SinPrivilegio. EcoApiKtor.respuesta llama expirada solo para 401; GestorSesion.limpiar borra credencial y BaseLocal. Un 403 conserva sesión y la operación pendiente con su error.

Demostración y resultado esperado: Ejecutar erroresHttpDistinguenValidacion401403409 y rechazo403ConservaOperacionYNoCierraSesion. La UI vuelve al login con 401 y conserva perfil con 403.

## 6. ¿Cómo llega un registro hasta el píxel?

PostgreSQL/pesaje → EcoRepository.mapearPesaje → EcoService.pesajes → EcoController → JSON → EcoApiKtor.pesajes → PesajeDto → EcoRepositorioOffline.sincronizar → BaseLocal.guardar → Eco.sq/PesajeLocal → asFlow → EcoViewModel.pesajes → EcoApp.Detalle → Text del neto.

Demostración y resultado esperado: Registrar un pesaje, observar la fila en PostgreSQL y la tarjeta Android. El ID negativo se sustituye por el definitivo al aceptar el servidor; el neto definitivo procede de EcoAPI.

## 7. ¿Por qué observar SQLDelight?

data/local/BaseLocal.kt usa Query.asFlow().mapToList y EcoViewModel usa stateIn/StateFlow. Eco.sq contiene rutas, puntos y pesajes. La fuente local evita que una pérdida de cobertura elimine el contenido y mantiene consistente la UI con escrituras locales.

Demostración y resultado esperado: Activar modo avión con la ruta previamente sincronizada. Las capturas 07, 08 y 09 muestran datos y registro disponibles sin red.

## 8. ¿Qué se guarda sin conexión?

EcoRepositorioOffline.guardar ejecuta una transacción con PesajeLocal y Operacion. siguienteId usa min/coalesce y devuelve -1 en vacío. pendiente=true y solicitudId identifica la operación. El neto se muestra como estimación; EscrituraPesajeDto no tiene pesoNeto.

Demostración y resultado esperado: Evidencia real: offline-verificacion.txt contiene (-1,42.0,2.0,1,Offline-Jorge-20261028). EcoOfflineTest verifica también -2 para la segunda fila.

## 9. ¿En qué orden sincroniza?

domain/usecase/SincronizarUseCase invoca EcoRepositorioOffline.sincronizar. Primero envía la cola, verifica cada respuesta y reemplaza ID/referencias en una transacción; después descarga ruta y páginas y actualiza SQLDelight. El mutex evita ejecuciones solapadas. Descargar primero podría sobrescribir cambios locales; por eso además se protegen filas pendientes.

Demostración y resultado esperado: sincronizacionSubeAntesDeDescargarYSustituyeEnTransaccion verifica crear → ruta → pesajes. respuestaPerdidaYEdicionPosteriorNoDuplicanNiPierdenDatos prueba el reintento con UUID estable.

## 10. ¿Qué sustituyen los commonTest?

shared/src/commonTest/.../EcoOfflineTest.kt usa MockEngine para HTTP y ApiPrueba solo como doble de red del repositorio. driverPrueba tiene actual en jvmTest y androidHostTest con JdbcSqliteDriver.IN_MEMORY. Se usa el mismo EcoDatabase.Schema de producción, no un repositorio en memoria como runtime.

Demostración y resultado esperado: Ejecutar ./gradlew.bat :shared:jvmTest y abrir shared/build/reports/tests/jvmTest/index.html. Resultados_CommonTest_JorgeQuispe.txt enumera las nueve pruebas reales aprobadas.

## 11. ¿Cómo llega una validación al campo?

EcoAPI: @Valid/Bean Validation y RN-03 generan ErrorDto.validationErrors en ManejadorDeErrores. EcoRutas: ErrorDto → MapeoErrores.Validacion → ResumenSync.campos o ErrorApiException → EcoViewModel.formulario.errores → OutlinedTextField.supportingText.

Demostración y resultado esperado: Introducir cuatro decimales en el bruto: EcoAPI rechaza por @Digits y Android muestra el mensaje debajo del campo. Captura 13. Corregir a tres decimales repara la misma operación en cola.

## 12. ¿Cómo incorporar otro rol?

EcoAPI: añadir migración Flyway para rol/privilegio/rol_privilegio y asignar usuario. UsuarioRepository.buscar recupera permisos de base; JwtService los incluye en nuevos tokens. No se agregan if por nombre del rol a EcoController ni al filtro. Android respeta el Set<String> de privilegios. Si aparece una operación completamente nueva, añadir contrato, servicio, cliente y acción por ese privilegio.

Demostración y resultado esperado: La solicitud SC-B no se implementó: el docente aún no asignó la Parte II. Mostrar V1__esquema.sql y los tres roles vigentes; no inventar un cuarto.

## 13. ¿Qué demuestra el historial y qué conflictos hubo?

Ambos repositorios conservan main, develop, feature/* y fix/* con PR y merge commits. El trabajo real comenzó el 28/10/2026 y no demuestra jornadas previas. No hubo un conflicto de merge real en esta sesión; no se inventa uno. Ante un conflicto se actualiza y resuelve en la rama de funcionalidad, se comprueba y luego se integra por PR.

Demostración y resultado esperado: Ejecutar git log --graph --oneline --decorate --all y git log --date=short --pretty="%h %ad %s". Ver los PR de GitHub. Parte II y sus tres commits en 80 minutos están pendientes de la asignación real.

