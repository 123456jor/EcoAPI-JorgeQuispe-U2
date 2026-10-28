package pe.upeu.ecorecicla.config
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean fun passwordEncoder() = BCryptPasswordEncoder(12)
    @Bean fun seguridad(http: HttpSecurity,filter: JwtAuthenticationFilter,mapper: ObjectMapper): SecurityFilterChain {
        http.csrf { it.disable() }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/auth/login").permitAll().anyRequest().authenticated() }
            .exceptionHandling { it.authenticationEntryPoint { req,res,_ -> escribirError(res,mapper,401,"Token requerido",req.requestURI) }.accessDeniedHandler { req,res,_ -> escribirError(res,mapper,403,"No posee el privilegio requerido",req.requestURI) } }
            .addFilterBefore(filter,UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
