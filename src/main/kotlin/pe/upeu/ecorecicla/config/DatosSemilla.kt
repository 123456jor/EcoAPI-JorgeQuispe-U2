package pe.upeu.ecorecicla.config
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.time.*
@Component
@ConditionalOnProperty(name=["eco.seed-enabled"],havingValue="true")
class DatosSemilla(private val db: JdbcTemplate,private val encoder: BCryptPasswordEncoder,@Value("\${eco.seed-password}") private val clave: String): ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        require(clave.length>=8) { "SEED_PASSWORD debe configurarse fuera del repositorio" }
        db.execute("SELECT pg_advisory_xact_lock(20261027)")
        val nombres=listOf(Triple("operario1","Jorge Quispe — Operario","OPERARIO"),Triple("supervisor1","Supervisor de operaciones","SUPERVISOR"),Triple("admin","Administrador EcoRecicla","ADMINISTRADOR"))
        nombres.forEach { (usuario,nombre,rol) -> db.update("INSERT INTO usuario(usuario,nombre,clave,rol_id) SELECT ?,?,?,id FROM rol WHERE nombre=? ON CONFLICT(usuario) DO NOTHING",usuario,nombre,encoder.encode(clave),rol) }
        if(db.queryForObject("SELECT count(*) FROM punto",Long::class.java)==0L) {
            val empresas=listOf("Mercado Las Flores","Colegio San Miguel","Comercial El Roble","Imprenta Horizonte","Bodega La Esperanza","Almacén Los Olivos","Restaurante El Valle","Centro Cultural Norte","Textiles Aurora","Distribuidora Andina","Hotel Los Jardines","Oficinas Río Verde")
            empresas.forEachIndexed { i,nombre -> db.update("INSERT INTO punto(razon_social,direccion,zona,material_habitual) VALUES (?,?,?,?)",nombre,"Av. Reciclaje ${100+i*25}",listOf("Norte","Centro","Sur")[i/4],listOf("Cartón","Plástico","Metal")[i%3]) }
        }
        val hoy=LocalDate.now(ZoneId.of("America/Lima"))
        val operario=db.queryForObject("SELECT id FROM usuario WHERE usuario='operario1'",Long::class.java)!!
        val supervisor=db.queryForObject("SELECT id FROM usuario WHERE usuario='supervisor1'",Long::class.java)!!
        val puntos=db.queryForList("SELECT id FROM punto ORDER BY id LIMIT 12",Long::class.java)
        for((fecha,cerrada) in listOf(hoy.minusDays(1) to true,hoy to false)) {
            val etiqueta="demo-$fecha"
            if(db.queryForObject("SELECT count(*) FROM ruta WHERE etiqueta=?",Long::class.java,etiqueta)!!>0L) continue
            val ruta=db.queryForObject("INSERT INTO ruta(fecha,estado,etiqueta) VALUES (?,?,?) RETURNING id",Long::class.java,fecha,if(cerrada) "CERRADA" else "ABIERTA",etiqueta)!!
            listOf(operario,supervisor).forEach { db.update("INSERT INTO ruta_usuario VALUES (?,?)",ruta,it) }
            val plan=if(cerrada) puntos.takeLast(6) else puntos.take(6)
            plan.forEachIndexed { i,id -> db.update("INSERT INTO ruta_punto VALUES (?,?,?)",ruta,id,i+1) }
            val cantidad=if(cerrada) 12 else 3
            repeat(cantidad) { i ->
                val punto=plan[i%plan.size]
                val autor=if(i==1) supervisor else operario
                val momento=(if(!cerrada && i==2) fecha.minusDays(1) else fecha).atTime(8+i%8,15).atZone(ZoneId.of("America/Lima")).toOffsetDateTime()
                db.update("INSERT INTO pesaje(ruta_id,punto_id,fecha_hora,material,peso_bruto,tara,peso_neto,observacion,usuario_id,anulado,motivo_anulacion) VALUES (?,?,?,'Cartón',25,2,23,'Dato semilla del examen',?,?,?)",ruta,punto,momento,autor,cerrada && i==0,if(cerrada && i==0) "Error de digitación — semilla" else null)
            }
        }
    }
}
