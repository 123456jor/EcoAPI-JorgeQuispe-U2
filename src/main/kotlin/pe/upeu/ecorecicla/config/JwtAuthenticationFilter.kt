package pe.upeu.ecorecicla.config
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtException
import pe.upeu.ecorecicla.seguridad.*
@Component
class JwtAuthenticationFilter(private val jwt: JwtService, private val usuarios: UsuarioRepository, private val mapper: ObjectMapper): OncePerRequestFilter() {
    override fun shouldNotFilter(request: HttpServletRequest) = request.method=="POST" && request.requestURI=="/api/v1/auth/login"
    override fun doFilterInternal(request: HttpServletRequest,response: HttpServletResponse,chain: FilterChain) {
        val header=request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) { escribirError(response,mapper,401,"Token requerido",request.requestURI); return }
        try {
            val token=jwt.validar(header.removePrefix("Bearer "))
            val user=usuarios.porId(token.subject.toLong()) ?: throw IllegalArgumentException()
            val signed=token.getClaimAsStringList("privilegios").toSet()
            val permisos=user.rol.privilegios.intersect(signed)
            val principal=Identidad(user.id,user.usuario,permisos)
            SecurityContextHolder.getContext().authentication=UsernamePasswordAuthenticationToken(principal,null,permisos.map(::SimpleGrantedAuthority))
        } catch(e: JwtException) { escribirError(response,mapper,401,"Token inválido o vencido",request.requestURI); return
        } catch(e: IllegalArgumentException) { escribirError(response,mapper,401,"Token inválido",request.requestURI); return
        } catch(e: Exception) { escribirError(response,mapper,500,"No se pudo comprobar la sesión. Intente nuevamente.",request.requestURI); return }
        chain.doFilter(request,response)
    }
}
