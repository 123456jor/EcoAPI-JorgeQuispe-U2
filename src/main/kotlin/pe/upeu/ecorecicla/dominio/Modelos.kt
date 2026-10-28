package pe.upeu.ecorecicla.dominio
import java.math.BigDecimal
data class PuntoAcopio(val id: Long,val razonSocial: String,val direccion: String,val zona: String,val materialHabitual: String,val secuencia: Int = 0,val atendido: Boolean = false,val motivoOmision: String? = null)
data class Ruta(val id: Long,val fecha: String,val estado: String,val puntos: List<PuntoAcopio>)
data class Pesaje(val id: Long,val rutaId: Long,val puntoId: Long,val fechaHora: String,val material: String,val pesoBruto: BigDecimal,val tara: BigDecimal,val pesoNeto: BigDecimal,val operario: String,val estado: String,val motivoAnulacion: String?,val observacion: String)
