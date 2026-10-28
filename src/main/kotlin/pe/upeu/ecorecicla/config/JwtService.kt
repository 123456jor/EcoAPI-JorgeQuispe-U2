package pe.upeu.ecorecicla.config
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.stereotype.Service
import pe.upeu.ecorecicla.seguridad.Usuario
import javax.crypto.spec.SecretKeySpec
import com.nimbusds.jose.jwk.source.ImmutableSecret
import java.time.Instant
@Service
class JwtService(@Value("\${eco.jwt-secret}") secret: String) {
    private val key = SecretKeySpec(secret.toByteArray(),"HmacSHA256")
    private val encoder = NimbusJwtEncoder(ImmutableSecret(key))
    private val decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().also {
        it.setJwtValidator(org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator(JwtTimestampValidator(java.time.Duration.ZERO), JwtIssuerValidator("EcoAPI")))
    }
    init { require(secret.toByteArray().size >= 32) { "JWT_SECRET requiere al menos 32 bytes" } }
    fun emitir(u: Usuario): Jwt {
        val now=Instant.now()
        val claims=JwtClaimsSet.builder().issuer("EcoAPI").subject(u.id.toString()).issuedAt(now).expiresAt(now.plusSeconds(3600)).claim("usuario",u.usuario).claim("privilegios",u.rol.privilegios.toList()).build()
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims))
    }
    fun validar(token: String): Jwt = decoder.decode(token)
}
