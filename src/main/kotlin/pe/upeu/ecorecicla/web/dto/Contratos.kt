package pe.upeu.ecorecicla.web.dto
import jakarta.validation.constraints.NotBlank
data class LoginRequest(@field:NotBlank val usuario: String, @field:NotBlank val contrasena: String)
data class UsuarioDto(val id: Long, val usuario: String, val nombre: String, val rol: String, val privilegios: Set<String>)
data class SesionDto(val token: String, val usuario: String, val nombre: String, val rol: String, val privilegios: Set<String>, val expiraEn: String)
data class Pagina<T>(val contenido: List<T>, val pagina: Int, val tamanio: Int, val totalElementos: Long, val totalPaginas: Int, val ultima: Boolean)
data class ErrorDto(val timestamp: String, val status: Int, val error: String, val message: String, val path: String, val validationErrors: Map<String,String> = emptyMap())
