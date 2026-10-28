package pe.upeu.ecorecicla.web.dto
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
data class PesajeRequest(val solicitudId: UUID,val rutaId: Long,val puntoId: Long,@field:NotBlank @field:Size(max=100) val material: String,@field:Digits(integer=11,fraction=3) val pesoBruto: BigDecimal,@field:DecimalMin("0") @field:Digits(integer=11,fraction=3) val tara: BigDecimal,@field:Size(max=500) val observacion: String = "")
data class MotivoRequest(@field:NotBlank @field:Size(max=500) val motivo: String)
data class PuntoRequest(@field:NotBlank @field:Size(max=200) val razonSocial: String,@field:NotBlank @field:Size(max=300) val direccion: String,@field:NotBlank @field:Size(max=80) val zona: String,@field:NotBlank @field:Size(max=100) val materialHabitual: String)
data class RutaRequest(val fecha: LocalDate,@field:NotEmpty val usuarios: List<Long>,@field:NotEmpty val puntos: List<Long>)
