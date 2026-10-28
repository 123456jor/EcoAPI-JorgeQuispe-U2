package pe.upeu.ecorecicla.web
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.http.ResponseEntity
import pe.upeu.ecorecicla.seguridad.Identidad
import pe.upeu.ecorecicla.servicio.EcoService
import pe.upeu.ecorecicla.web.dto.*
import java.net.URI
import java.time.LocalDate
import java.time.ZoneId
@RestController
@RequestMapping("/api/v1")
class EcoController(private val servicio: EcoService) {
    @GetMapping("/rutas/hoy") fun ruta(@AuthenticationPrincipal u: Identidad,@RequestParam(required=false) fecha: LocalDate?) = servicio.rutaDelDia(u,fecha ?: LocalDate.now(ZoneId.of("America/Lima")))
    @GetMapping("/puntos") fun puntos(@RequestParam(defaultValue="0") pagina: Int,@RequestParam(defaultValue="20") tamanio: Int,@RequestParam(required=false) zona: String?) = servicio.puntos(pagina,tamanio,zona)
    @GetMapping("/puntos/{id}/pesajes") fun pesajes(@AuthenticationPrincipal u: Identidad,@PathVariable id: Long,@RequestParam(defaultValue="0") pagina: Int,@RequestParam(defaultValue="20") tamanio: Int) = servicio.pesajes(u,id,pagina,tamanio)
    @PostMapping("/pesajes") fun crear(@AuthenticationPrincipal u: Identidad,@Valid @RequestBody p: PesajeRequest) = servicio.crear(u,p).let { ResponseEntity.created(URI.create("/api/v1/pesajes/${it.id}")).body(it) }
    @PutMapping("/pesajes/{id}") fun editar(@AuthenticationPrincipal u: Identidad,@PathVariable id: Long,@Valid @RequestBody p: PesajeRequest) = servicio.editar(u,id,p)
    @DeleteMapping("/pesajes/{id}") fun anular(@AuthenticationPrincipal u: Identidad,@PathVariable id: Long,@RequestParam motivo: String): ResponseEntity<Void> { servicio.anular(u,id,motivo); return ResponseEntity.noContent().build() }
    @PutMapping("/rutas/{ruta}/puntos/{punto}/omision") fun omitir(@AuthenticationPrincipal u: Identidad,@PathVariable ruta: Long,@PathVariable punto: Long,@Valid @RequestBody p: MotivoRequest): ResponseEntity<Void> { servicio.omitir(u,ruta,punto,p.motivo); return ResponseEntity.noContent().build() }
    @PostMapping("/rutas/{id}/cerrar") fun cerrar(@AuthenticationPrincipal u: Identidad,@PathVariable id: Long): ResponseEntity<Void> { servicio.cerrar(u,id); return ResponseEntity.noContent().build() }
    @PostMapping("/puntos") fun crearPunto(@Valid @RequestBody p: PuntoRequest) = servicio.guardarPunto(null,p).let { ResponseEntity.created(URI.create("/api/v1/puntos/${it.id}")).body(it) }
    @PutMapping("/puntos/{id}") fun editarPunto(@PathVariable id: Long,@Valid @RequestBody p: PuntoRequest) = servicio.guardarPunto(id,p)
    @PostMapping("/rutas") fun crearRuta(@Valid @RequestBody p: RutaRequest) = servicio.crearRuta(p).let { ResponseEntity.created(URI.create("/api/v1/rutas/${it.id}")).body(it) }
}
