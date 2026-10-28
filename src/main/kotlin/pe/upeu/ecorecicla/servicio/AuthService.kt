package pe.upeu.ecorecicla.servicio
import org.springframework.stereotype.Service
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.authentication.BadCredentialsException
import pe.upeu.ecorecicla.seguridad.*
import pe.upeu.ecorecicla.config.*
import pe.upeu.ecorecicla.web.dto.*
@Service
class AuthService(private val usuarios: UsuarioRepository, private val encoder: BCryptPasswordEncoder, private val jwt: JwtService) {
    // También verifica BCrypt para usuarios inexistentes: evita diferencias triviales de tiempo.
    private val dummy = encoder.encode(java.util.UUID.randomUUID().toString())
    fun login(p: LoginRequest): SesionDto {
        val u=usuarios.porNombre(p.usuario)
        val coincide=encoder.matches(p.contrasena,u?.clave ?: dummy)
        if(u==null || !coincide) throw BadCredentialsException("Credenciales incorrectas")
        val token=jwt.emitir(u)
        return SesionDto(token.tokenValue,u.usuario,u.nombre,u.rol.nombre,u.rol.privilegios,requireNotNull(token.expiresAt).toString())
    }
    fun actual(id: Long): UsuarioDto {
        val u=usuarios.porId(id) ?: throw ReglaException(401,"Sesión inválida")
        return UsuarioDto(u.id,u.usuario,u.nombre,u.rol.nombre,u.rol.privilegios)
    }
}
