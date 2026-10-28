package pe.upeu.ecorecicla.config
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import pe.upeu.ecorecicla.web.dto.ErrorDto
import java.time.OffsetDateTime
class ReglaException(val status: Int, override val message: String, val campos: Map<String,String> = emptyMap()) : RuntimeException(message)
fun errorDto(status: Int, message: String, path: String, campos: Map<String,String> = emptyMap()) = ErrorDto(OffsetDateTime.now().toString(),status,HttpStatus.valueOf(status).reasonPhrase,message,path,campos)
fun escribirError(response: HttpServletResponse, mapper: ObjectMapper, status: Int, message: String, path: String) {
    response.status=status; response.contentType="application/json"; response.characterEncoding="UTF-8"
    mapper.writeValue(response.writer,errorDto(status,message,path))
}
@RestControllerAdvice
class ManejadorDeErrores {
    @ExceptionHandler(ReglaException::class)
    fun regla(e: ReglaException,r: HttpServletRequest) = ResponseEntity.status(e.status).body(errorDto(e.status,e.message,r.requestURI,e.campos))
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validacion(e: MethodArgumentNotValidException,r: HttpServletRequest) = ResponseEntity.badRequest().body(errorDto(400,"Datos inválidos",r.requestURI,e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Dato inválido") }))
    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun formato(e: Exception,r: HttpServletRequest) = ResponseEntity.badRequest().body(errorDto(400,"Formato de solicitud inválido",r.requestURI))
    @ExceptionHandler(AccessDeniedException::class)
    fun prohibido(e: Exception,r: HttpServletRequest) = ResponseEntity.status(403).body(errorDto(403,"No posee el privilegio requerido",r.requestURI))
    @ExceptionHandler(AuthenticationException::class)
    fun autenticacion(e: Exception,r: HttpServletRequest) = ResponseEntity.status(401).body(errorDto(401,"Credenciales incorrectas",r.requestURI))
    @ExceptionHandler(Exception::class)
    fun inesperado(e: Exception,r: HttpServletRequest): ResponseEntity<ErrorDto> {
        org.slf4j.LoggerFactory.getLogger(javaClass).error("Fallo interno en {}: {}",r.requestURI,e.javaClass.simpleName)
        return ResponseEntity.status(500).body(errorDto(500,"Error interno. Intente nuevamente.",r.requestURI))
    }
}
