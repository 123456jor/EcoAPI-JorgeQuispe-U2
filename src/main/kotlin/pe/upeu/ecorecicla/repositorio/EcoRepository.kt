package pe.upeu.ecorecicla.repositorio
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import pe.upeu.ecorecicla.dominio.*
import pe.upeu.ecorecicla.config.ReglaException
import java.sql.ResultSet
@Repository
class EcoRepository(val db: JdbcTemplate) {
    fun pesaje(id: Long): Pesaje = db.query("SELECT p.*,u.usuario FROM pesaje p JOIN usuario u ON u.id=p.usuario_id WHERE p.id=?", { r,_ -> mapearPesaje(r) },id).firstOrNull() ?: throw ReglaException(404,"Pesaje no encontrado")
    fun mapearPesaje(r: ResultSet) = Pesaje(r.getLong("id"),r.getLong("ruta_id"),r.getLong("punto_id"),r.getTimestamp("fecha_hora").toInstant().toString(),r.getString("material"),r.getBigDecimal("peso_bruto"),r.getBigDecimal("tara"),r.getBigDecimal("peso_neto"),r.getString("usuario"),if(r.getBoolean("anulado")) "ANULADO" else "REGISTRADO",r.getString("motivo_anulacion"),r.getString("observacion"))
    fun punto(r: ResultSet) = PuntoAcopio(r.getLong("id"),r.getString("razon_social"),r.getString("direccion"),r.getString("zona"),r.getString("material_habitual"))
    fun puntosRuta(id: Long) = db.query("""
        SELECT p.*,rp.secuencia,EXISTS(SELECT 1 FROM pesaje x WHERE x.ruta_id=rp.ruta_id AND x.punto_id=p.id AND NOT x.anulado) atendido,o.motivo
        FROM ruta_punto rp JOIN punto p ON p.id=rp.punto_id LEFT JOIN omision o ON o.ruta_id=rp.ruta_id AND o.punto_id=rp.punto_id
        WHERE rp.ruta_id=? ORDER BY rp.secuencia
    """, { r,_ -> punto(r).copy(secuencia=r.getInt("secuencia"),atendido=r.getBoolean("atendido"),motivoOmision=r.getString("motivo")) }, id)
}
