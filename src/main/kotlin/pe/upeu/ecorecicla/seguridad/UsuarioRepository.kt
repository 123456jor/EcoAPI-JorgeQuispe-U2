package pe.upeu.ecorecicla.seguridad
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
@Repository
class UsuarioRepository(private val db: JdbcTemplate) {
    fun porNombre(nombre: String): Usuario? = buscar("u.usuario = ?", nombre)
    fun porId(id: Long): Usuario? = buscar("u.id = ?", id)
    private fun buscar(condicion: String, valor: Any): Usuario? {
        val rows = db.queryForList("SELECT u.*,r.nombre rol FROM usuario u JOIN rol r ON r.id=u.rol_id WHERE u.activo AND $condicion", valor)
        val u = rows.firstOrNull() ?: return null
        val permisos = db.queryForList("SELECT privilegio FROM rol_privilegio WHERE rol_id=?", String::class.java, u["rol_id"]).toSet()
        return Usuario((u["id"] as Number).toLong(), u["usuario"] as String, u["nombre"] as String, u["clave"] as String, Rol(u["rol"] as String,permisos))
    }
}
