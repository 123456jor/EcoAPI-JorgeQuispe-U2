package pe.upeu.ecorecicla.web.dto
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
data class PesajeRequest(val solicitudId: UUID,val rutaId: Long,val puntoId: Long,@field:NotBlank(message="Este campo es obligatorio") @field:Size(max=100,message="Máximo 100 caracteres") val material: String,@field:Digits(integer=11,fraction=3,message="Use hasta 11 dígitos enteros y 3 decimales") val pesoBruto: BigDecimal,@field:DecimalMin(value="0",message="La tara no puede ser negativa") @field:Digits(integer=11,fraction=3,message="Use hasta 11 dígitos enteros y 3 decimales") val tara: BigDecimal,@field:Size(max=500,message="Máximo 500 caracteres") val observacion: String = "",@field:DecimalMin("0") @field:DecimalMax("100") @field:Digits(integer=3,fraction=2) val humedadEstimada: BigDecimal = BigDecimal.ZERO)
data class MotivoRequest(@field:NotBlank(message="Este campo es obligatorio") @field:Size(max=500,message="Máximo 500 caracteres") val motivo: String)
data class PuntoRequest(@field:NotBlank(message="Este campo es obligatorio") @field:Size(max=200,message="Máximo 200 caracteres") val razonSocial: String,@field:NotBlank(message="Este campo es obligatorio") @field:Size(max=300,message="Máximo 300 caracteres") val direccion: String,@field:NotBlank(message="Este campo es obligatorio") @field:Size(max=80,message="Máximo 80 caracteres") val zona: String,@field:NotBlank(message="Este campo es obligatorio") @field:Size(max=100,message="Máximo 100 caracteres") val materialHabitual: String)
data class RutaRequest(val fecha: LocalDate,@field:NotEmpty(message="Incluya al menos un elemento") val usuarios: List<Long>,@field:NotEmpty(message="Incluya al menos un elemento") val puntos: List<Long>)
