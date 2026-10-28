package pe.upeu.ecorecicla
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
@SpringBootApplication(exclude=[org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration::class])
class Aplicacion
fun main(args: Array<String>) { runApplication<Aplicacion>(*args) }
