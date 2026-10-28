package pe.upeu.ecorecicla.servicio
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pe.upeu.ecorecicla.config.ReglaException
import pe.upeu.ecorecicla.dominio.*
import pe.upeu.ecorecicla.repositorio.EcoRepository
import pe.upeu.ecorecicla.seguridad.Identidad
import pe.upeu.ecorecicla.web.dto.*
import java.time.*
import java.security.MessageDigest
import java.util.UUID
@Service
@Transactional
class EcoService(private val repo: EcoRepository,private val json: ObjectMapper) {
    private val db get()=repo.db
    private val zona=ZoneId.of("America/Lima")
    private fun existe(sql: String,vararg args: Any) = (db.queryForObject(sql,Long::class.java,*args) ?: 0)>0
    private fun acceso(u: Identidad,ruta: Long) {
        if(u.tiene("RUTA_CERRAR") || u.tiene("PUNTO_GESTIONAR")) return
        if(!existe("SELECT count(*) FROM ruta_usuario WHERE ruta_id=? AND usuario_id=?",ruta,u.id)) throw ReglaException(403,"La ruta no está asignada a su usuario")
    }
    private fun bloquearRuta(id: Long): String = db.queryForList("SELECT estado FROM ruta WHERE id=? FOR UPDATE",id).firstOrNull()?.get("estado") as? String ?: throw ReglaException(404,"Ruta no encontrada")
    private fun abierta(estado: String) { if(estado!="ABIERTA") throw ReglaException(409,"La ruta está cerrada; no admite cambios") }
    private fun pagina(p: Int,t: Int) { if(p<0 || t !in 1..100) throw ReglaException(400,"Paginación inválida",mapOf("pagina" to "Debe ser >= 0","tamanio" to "Entre 1 y 100")) }
    private fun <T> envolver(items: List<T>,p: Int,t: Int,total: Long) = Pagina(items,p,t,total,((total+t-1)/t).toInt(),(p.toLong()+1)*t>=total)
    private fun huella(tipo: String,id: Long?,p: PesajeRequest) = MessageDigest.getInstance("SHA-256").digest("$tipo|$id|$p".toByteArray()).joinToString("") { "%02x".format(it) }
    private fun anterior(u: Identidad,p: PesajeRequest,h: String): Pesaje? {
        val r=db.queryForList("SELECT huella,respuesta FROM peticion WHERE usuario_id=? AND solicitud=?",u.id,p.solicitudId).firstOrNull() ?: return null
        if(r["huella"]!=h) throw ReglaException(409,"El identificador de solicitud ya fue utilizado con otros datos")
        return json.readValue(r["respuesta"] as String,Pesaje::class.java)
    }
    private fun recordar(u: Identidad,p: PesajeRequest,h: String,pesaje: Pesaje) {
        db.update("INSERT INTO peticion(usuario_id,solicitud,huella,respuesta) VALUES (?,?,?,?)",u.id,p.solicitudId,h,json.writeValueAsString(pesaje))
    }
    private fun auditar(u: Identidad,accion: String,antes: Pesaje?,despues: Pesaje) {
        db.update("INSERT INTO auditoria_pesaje(pesaje_id,actor_id,accion,antes,despues) VALUES (?,?,?,?::jsonb,?::jsonb)",despues.id,u.id,accion,antes?.let { json.writeValueAsString(it) },json.writeValueAsString(despues))
    }
    private fun validar(p: PesajeRequest): java.math.BigDecimal {
        val neto=p.pesoBruto-p.tara
        if(neto<=java.math.BigDecimal.ZERO) throw ReglaException(400,"Datos inválidos",mapOf("pesoBruto" to "El peso neto debe ser mayor que cero"))
        if(!existe("SELECT count(*) FROM ruta_punto WHERE ruta_id=? AND punto_id=?",p.rutaId,p.puntoId)) throw ReglaException(400,"El punto no pertenece a la ruta",mapOf("puntoId" to "Punto fuera de la planificación"))
        return neto
    }
    @PreAuthorize("hasAuthority('PUNTO_LEER')")
    fun rutaDelDia(u: Identidad,fecha: LocalDate): Ruta {
        val r=db.queryForList("SELECT r.* FROM ruta r WHERE (? OR EXISTS(SELECT 1 FROM ruta_usuario ru WHERE ru.ruta_id=r.id AND ru.usuario_id=?)) AND r.fecha=? ORDER BY r.id DESC LIMIT 1",u.tiene("LECTURA_GLOBAL"),u.id,fecha).firstOrNull() ?: throw ReglaException(404,"No hay ruta asignada para esta fecha")
        val id=(r["id"] as Number).toLong()
        return Ruta(id,r["fecha"].toString(),r["estado"] as String,repo.puntosRuta(id))
    }
    @PreAuthorize("hasAuthority('PUNTO_LEER')")
    fun puntos(p: Int,t: Int,zona: String?): Pagina<PuntoAcopio> {
        pagina(p,t)
        val filtro=zona?.trim().orEmpty()
        val total=db.queryForObject("SELECT count(*) FROM punto WHERE activo AND (?='' OR zona=?)",Long::class.java,filtro,filtro)!!
        return envolver(db.query("SELECT * FROM punto WHERE activo AND (?='' OR zona=?) ORDER BY id LIMIT ? OFFSET ?",{ r,_ -> repo.punto(r) },filtro,filtro,t,p.toLong()*t),p,t,total)
    }
    @PreAuthorize("hasAuthority('PESAJE_LEER')")
    fun pesajes(u: Identidad,punto: Long,p: Int,t: Int): Pagina<Pesaje> {
        pagina(p,t)
        if(!existe("SELECT count(*) FROM punto WHERE id=?",punto)) throw ReglaException(404,"Punto no encontrado")
        val global=u.tiene("LECTURA_GLOBAL") || u.tiene("RUTA_CERRAR") || u.tiene("PUNTO_GESTIONAR")
        val filtro="p.punto_id=? AND (? OR EXISTS(SELECT 1 FROM ruta_usuario ru WHERE ru.ruta_id=p.ruta_id AND ru.usuario_id=?))"
        val total=db.queryForObject("SELECT count(*) FROM pesaje p WHERE $filtro",Long::class.java,punto,global,u.id)!!
        val rows=db.query("SELECT p.*,u.usuario FROM pesaje p JOIN usuario u ON u.id=p.usuario_id WHERE $filtro ORDER BY p.fecha_hora DESC,p.id DESC LIMIT ? OFFSET ?",{ r,_ -> repo.mapearPesaje(r) },punto,global,u.id,t,p.toLong()*t)
        return envolver(rows,p,t,total)
    }
    @PreAuthorize("hasAuthority('PESAJE_CREAR')")
    fun crear(u: Identidad,p: PesajeRequest): Pesaje {
        val estado=bloquearRuta(p.rutaId); acceso(u,p.rutaId)
        val h=huella("CREAR",null,p); anterior(u,p,h)?.let { return it }
        abierta(estado); val neto=validar(p)
        val id=db.queryForObject("INSERT INTO pesaje(ruta_id,punto_id,material,peso_bruto,tara,peso_neto,observacion,usuario_id) VALUES (?,?,?,?,?,?,?,?) RETURNING id",Long::class.java,p.rutaId,p.puntoId,p.material,p.pesoBruto,p.tara,neto,p.observacion,u.id)!!
        val resultado=repo.pesaje(id); auditar(u,"CREAR",null,resultado); recordar(u,p,h,resultado); return resultado
    }
    @PreAuthorize("hasAuthority('PESAJE_EDITAR')")
    fun editar(u: Identidad,id: Long,p: PesajeRequest): Pesaje {
        val estado=bloquearRuta(p.rutaId); acceso(u,p.rutaId)
        val h=huella("EDITAR",id,p); anterior(u,p,h)?.let { return it }
        val old=repo.pesaje(id)
        if(old.rutaId!=p.rutaId || old.puntoId!=p.puntoId) throw ReglaException(400,"No se puede cambiar la ruta o el punto de un pesaje")
        abierta(estado)
        if(!u.tiene("PESAJE_ANULAR") && (old.operario!=u.usuario || Instant.parse(old.fechaHora).atZone(zona).toLocalDate()!=LocalDate.now(zona))) throw ReglaException(403,"Solo puede corregir sus pesajes del mismo día")
        if(old.estado=="ANULADO") throw ReglaException(409,"No se puede corregir un pesaje anulado")
        val neto=validar(p)
        db.update("UPDATE pesaje SET material=?,peso_bruto=?,tara=?,peso_neto=?,observacion=? WHERE id=?",p.material,p.pesoBruto,p.tara,neto,p.observacion,id)
        val result=repo.pesaje(id); auditar(u,"EDITAR",old,result); recordar(u,p,h,result); return result
    }
    @PreAuthorize("hasAuthority('PESAJE_ANULAR')")
    fun anular(u: Identidad,id: Long,motivo: String) {
        if(motivo.isBlank() || motivo.length>500) throw ReglaException(400,"Motivo obligatorio",mapOf("motivo" to "Entre 1 y 500 caracteres"))
        val old=repo.pesaje(id); abierta(bloquearRuta(old.rutaId)); acceso(u,old.rutaId)
        if(old.estado=="ANULADO") return
        db.update("UPDATE pesaje SET anulado=TRUE,motivo_anulacion=? WHERE id=?",motivo,id)
        auditar(u,"ANULAR",old,repo.pesaje(id))
    }
    @PreAuthorize("hasAuthority('PESAJE_CREAR')")
    fun omitir(u: Identidad,ruta: Long,punto: Long,motivo: String) {
        abierta(bloquearRuta(ruta)); acceso(u,ruta)
        if(!existe("SELECT count(*) FROM ruta_punto WHERE ruta_id=? AND punto_id=?",ruta,punto)) throw ReglaException(404,"Punto fuera de la ruta")
        db.update("INSERT INTO omision(ruta_id,punto_id,motivo,usuario_id) VALUES (?,?,?,?) ON CONFLICT(ruta_id,punto_id) DO UPDATE SET motivo=excluded.motivo,usuario_id=excluded.usuario_id,fecha_hora=now()",ruta,punto,motivo,u.id)
    }
    @PreAuthorize("hasAuthority('RUTA_CERRAR')")
    fun cerrar(u: Identidad,id: Long) {
        val estado=bloquearRuta(id); acceso(u,id)
        if(estado=="CERRADA") return
        val faltan=db.queryForObject("SELECT count(*) FROM ruta_punto rp WHERE ruta_id=? AND NOT EXISTS(SELECT 1 FROM pesaje p WHERE p.ruta_id=rp.ruta_id AND p.punto_id=rp.punto_id AND NOT p.anulado) AND NOT EXISTS(SELECT 1 FROM omision o WHERE o.ruta_id=rp.ruta_id AND o.punto_id=rp.punto_id)",Long::class.java,id)!!
        if(faltan>0) throw ReglaException(409,"No se puede cerrar: faltan $faltan puntos por pesar o justificar")
        db.update("UPDATE ruta SET estado='CERRADA',cerrada_por=?,cerrada_en=now() WHERE id=?",u.id,id)
    }
    @PreAuthorize("hasAuthority('PUNTO_GESTIONAR')")
    fun guardarPunto(id: Long?,p: PuntoRequest): PuntoAcopio {
        val real=if(id==null) db.queryForObject("INSERT INTO punto(razon_social,direccion,zona,material_habitual) VALUES (?,?,?,?) RETURNING id",Long::class.java,p.razonSocial,p.direccion,p.zona,p.materialHabitual)!! else {
            if(db.update("UPDATE punto SET razon_social=?,direccion=?,zona=?,material_habitual=? WHERE id=? AND activo",p.razonSocial,p.direccion,p.zona,p.materialHabitual,id)==0) throw ReglaException(404,"Punto no encontrado")
            id
        }
        return PuntoAcopio(real,p.razonSocial,p.direccion,p.zona,p.materialHabitual)
    }
    @PreAuthorize("hasAuthority('PUNTO_GESTIONAR')")
    fun crearRuta(p: RutaRequest): Ruta {
        if(p.puntos.distinct().size!=p.puntos.size || p.usuarios.distinct().size!=p.usuarios.size) throw ReglaException(400,"No repita puntos o usuarios")
        p.puntos.forEach { if(!existe("SELECT count(*) FROM punto WHERE id=? AND activo",it)) throw ReglaException(400,"Punto inexistente: $it") }
        p.usuarios.forEach { if(!existe("SELECT count(*) FROM usuario WHERE id=? AND activo",it)) throw ReglaException(400,"Usuario inexistente: $it") }
        val id=db.queryForObject("INSERT INTO ruta(fecha,estado) VALUES (?,'ABIERTA') RETURNING id",Long::class.java,p.fecha)!!
        p.usuarios.forEach { db.update("INSERT INTO ruta_usuario VALUES (?,?)",id,it) }
        p.puntos.forEachIndexed { i,x -> db.update("INSERT INTO ruta_punto VALUES (?,?,?)",id,x,i+1) }
        return Ruta(id,p.fecha.toString(),"ABIERTA",repo.puntosRuta(id))
    }
}
