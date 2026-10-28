package pe.upeu.ecorecicla.seguridad
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import pe.upeu.ecorecicla.servicio.AuthService
import pe.upeu.ecorecicla.web.dto.LoginRequest
@RestController
@RequestMapping("/api/v1")
class AuthController(private val servicio: AuthService) {
    @PostMapping("/auth/login") fun login(@Valid @RequestBody p: LoginRequest) = servicio.login(p)
    @GetMapping("/usuarios/me") fun me(@AuthenticationPrincipal u: Identidad) = servicio.actual(u.id)
}
