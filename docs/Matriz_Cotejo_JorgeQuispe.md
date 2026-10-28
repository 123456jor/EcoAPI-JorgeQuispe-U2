# Matriz de cotejo — Jorge Quispe

Verificación real del 28 de octubre de 2026. La Parte II y el historial anterior al inicio no se consideran completos.

| N.º | Estado | Evidencia |
|---|---|---|
| 1 | Verificado | docker compose up --build -d --wait; ambos contenedores healthy. docker-final.log. |
| 2 | Verificado | 10 EcoApiTest aprobados; login real de tres cuentas; Newman login 200 e incorrecto 401. |
| 3 | Verificado | JwtAuthenticationFilter exige Bearer salvo POST login; prueba sin token 401. |
| 4 | Verificado | Tests y Newman: firma alterada 401 y JWT correctamente firmado pero vencido 401. |
| 5 | Verificado | Newman 403; commonTest conserva sesión y cola tras rechazo. Autorización en EcoService. |
| 6 | Verificado | Newman comprueba contenido/pagina/tamanio/total/ultima y filtro de zona; páginas consumidas hasta ultima. |
| 7 | Verificado | RN-03 y Bean Validation 400; captura 13 muestra un mensaje real del servidor bajo el campo. |
| 8 | Verificado | AlmacenSeguroAndroid AES-GCM/AndroidKeyStore; preferencias reales solo datos cifrados e IV. keystore-verificacion.txt. |
| 9 | Verificado | ClienteHttp.defaultRequest; prueba MockEngine comprueba Bearer centralizado. |
| 10 | Verificado | Logout real: RutaLocal, PuntoLocal, PesajeLocal y Operacion en cero; preferencias vacías. Captura 15. |
| 11 | Verificado | Interfaz condicionada por privilegios; operario no tiene Anular/Cerrar ruta. Supervisor muestra esas acciones. Servidor impone 403 independientemente de la UI. |
| 12 | Verificado | BaseLocal asFlow -> EcoViewModel StateFlow -> EcoApp; pantalla real obtiene lista de SQLDelight. |
| 13 | Verificado | Modo avión real con Wi-Fi apagado, detalle y formulario disponibles. Capturas 07 y 08. |
| 14 | Verificado | Consulta directa de SQLite: ID -1, bruto 42, tara 2, pendiente=1. Captura 09 y offline-verificacion.txt. |
| 15 | Verificado | Vuelta de conectividad: ID definitivo 22, neto 40; edición posterior neto 42. PostgreSQL confirma una fila incluso tras repetir sync. Capturas 10 a 12 y commonTest de respuesta perdida. |
| 16 | Verificado | Nueve pruebas commonTest ejecutadas mediante :shared:jvmTest; cero fallos. Resultados_CommonTest_JorgeQuispe.txt. |
| 17 | No acreditable retrospectivamente | Hay ramas y PR reales en ambos repositorios. Todo el trabajo comenzó el 28/10/2026; no hay jornadas previas ni fechas fabricadas. El requisito de varias fechas sigue sin acreditarse. |
| 18 | Verificado | Solo el bootstrap documental se creó en main. Funcionalidades en feature/fix, PR con merge hacia develop y fusión de entrega hacia main. |
| 19 | Versión base de producto | Mensajes convencionales en español. v1.0-unidad2 identifica el commit de producto verificado; Git final y README documentan el alcance. No se mueve una etiqueta publicada silenciosamente. |
| 20 | Pendiente de asignación | El usuario confirmó que el docente aún no asignó SC-A/B/C/D. No hay rama sc-* ni tres commits ficticios durante un bloque que no ocurrió. |
