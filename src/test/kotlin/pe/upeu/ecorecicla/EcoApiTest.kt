package pe.upeu.ecorecicla
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.MediaType
import java.util.UUID
import java.time.*
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import javax.crypto.spec.SecretKeySpec
import com.nimbusds.jose.jwk.source.ImmutableSecret
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EcoApiTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var db: JdbcTemplate
    @Autowired lateinit var json: ObjectMapper
    @Value("\${eco.seed-password}") lateinit var password: String
    @Value("\${eco.jwt-secret}") lateinit var secret: String
    private lateinit var operario: String
    private lateinit var supervisor: String
    private lateinit var admin: String
    private var ruta=0L; private var punto=0L; private var otroPunto=0L
    private fun login(u: String): String {
        val r=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(mapOf("usuario" to u,"contrasena" to password)))).andReturn().response
        assertEquals(200,r.status,r.contentAsString); return json.readTree(r.contentAsString)["token"].asText()
    }
    @BeforeEach fun preparar() {
        operario=login("operario1");supervisor=login("supervisor1");admin=login("admin")
        val puntos=db.queryForList("SELECT id FROM punto ORDER BY id LIMIT 2",Long::class.java);punto=puntos[0];otroPunto=puntos[1]
        ruta=db.queryForObject("INSERT INTO ruta(fecha,estado) VALUES (CURRENT_DATE,'ABIERTA') RETURNING id",Long::class.java)!!
        db.update("INSERT INTO ruta_usuario SELECT ?,id FROM usuario WHERE usuario IN ('operario1','supervisor1')",ruta)
        db.update("INSERT INTO ruta_punto VALUES (?,?,1),(?,?,2)",ruta,punto,ruta,otroPunto)
    }
    private fun payload(bruto: Int=30,tara: Int=2,solicitud: String=UUID.randomUUID().toString()) = mapOf("solicitudId" to solicitud,"rutaId" to ruta,"puntoId" to punto,"material" to "Cartón","pesoBruto" to bruto,"tara" to tara,"observacion" to "Prueba automatizada")
    private fun crear(token: String=operario,p: Map<String,Any> = payload()) = mvc.perform(post("/api/v1/pesajes").header("Authorization","Bearer $token").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(p))).andReturn().response
    @Test fun `humedad valida porcentaje y persiste alta y correccion`() {
        for(valor in listOf(-1,101,12.555)) assertEquals(400,crear(p=payload()+mapOf("humedadEstimada" to valor)).status)
        val r=crear(p=payload()+mapOf("humedadEstimada" to 12.5));assertEquals(201,r.status,r.contentAsString)
        val id=json.readTree(r.contentAsString)["id"].asLong()
        assertEquals(12.5,json.readTree(r.contentAsString)["humedadEstimada"].asDouble())
        val edit=mvc.perform(put("/api/v1/pesajes/$id").header("Authorization","Bearer $operario").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload()+mapOf("humedadEstimada" to 20)))).andReturn().response
        assertEquals(200,edit.status,edit.contentAsString)
        assertEquals(20.0,db.queryForObject("SELECT humedad_estimada FROM pesaje WHERE id=?",Double::class.java,id))
    }
    @Test fun `login rechaza credenciales y el token dura una hora`() {
        val r=mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"usuario\":\"operario1\",\"contrasena\":\"incorrecta\"}")).andReturn().response
        assertEquals(401,r.status)
        val claims=json.readTree(java.util.Base64.getUrlDecoder().decode(operario.split('.')[1]));assertEquals(3600,claims["exp"].asLong()-claims["iat"].asLong())
        assertTrue(db.queryForObject("SELECT clave FROM usuario WHERE usuario='operario1'",String::class.java)!!.startsWith("$2"))
    }
    @Test fun `ausente alterado y vencido dan 401`() {
        assertEquals(401,mvc.perform(get("/api/v1/usuarios/me")).andReturn().response.status)
        val partes=operario.split('.');val corrupto=partes[0]+"."+partes[1]+"."+(if(partes[2][0]=='a') "b" else "a")+partes[2].drop(1)
        assertEquals(401,mvc.perform(get("/api/v1/usuarios/me").header("Authorization","Bearer $corrupto")).andReturn().response.status)
        val encoder=NimbusJwtEncoder(ImmutableSecret(SecretKeySpec(secret.toByteArray(),"HmacSHA256")))
        val claims=JwtClaimsSet.builder().issuer("EcoAPI").subject("1").issuedAt(Instant.now().minusSeconds(4000)).expiresAt(Instant.now().minusSeconds(1)).build()
        val expired=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).tokenValue
        assertEquals(401,mvc.perform(get("/api/v1/usuarios/me").header("Authorization","Bearer $expired")).andReturn().response.status)
    }
    @Test fun `matriz de permisos se impone en el servicio`() {
        assertEquals(403,crear(admin).status)
        val created=crear();assertEquals(201,created.status,created.contentAsString)
        val id=json.readTree(created.contentAsString)["id"].asLong()
        assertEquals(403,mvc.perform(delete("/api/v1/pesajes/$id").param("motivo","Prueba").header("Authorization","Bearer $operario")).andReturn().response.status)
        assertEquals(204,mvc.perform(delete("/api/v1/pesajes/$id").param("motivo","Duplicado").header("Authorization","Bearer $supervisor")).andReturn().response.status)
        assertTrue(db.queryForObject("SELECT anulado FROM pesaje WHERE id=?",Boolean::class.java,id)!!)
        assertEquals(2L,db.queryForObject("SELECT count(*) FROM auditoria_pesaje WHERE pesaje_id=?",Long::class.java,id))
        assertEquals(200,mvc.perform(get("/api/v1/usuarios/me").header("Authorization","Bearer $operario")).andReturn().response.status)
    }
    @Test fun `RN01 rechaza alta y correccion en ruta cerrada`() {
        val created=crear();val id=json.readTree(created.contentAsString)["id"].asLong()
        db.update("UPDATE ruta SET estado='CERRADA' WHERE id=?",ruta)
        assertEquals(409,crear().status)
        assertEquals(409,mvc.perform(put("/api/v1/pesajes/$id").header("Authorization","Bearer $supervisor").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload()))).andReturn().response.status)
    }
    @Test fun `RN02 solo permite correccion propia del dia`() {
        val created=crear(supervisor);val id=json.readTree(created.contentAsString)["id"].asLong()
        fun editar(token: String)=mvc.perform(put("/api/v1/pesajes/$id").header("Authorization","Bearer $token").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(payload()))).andReturn().response.status
        assertEquals(403,editar(operario));assertEquals(200,editar(supervisor))
        db.update("UPDATE pesaje SET usuario_id=(SELECT id FROM usuario WHERE usuario='operario1'),fecha_hora=now()-interval '1 day' WHERE id=?",id)
        assertEquals(403,editar(operario));assertEquals(200,editar(supervisor))
    }
    @Test fun `RN03 valida campos y calcula neto exclusivamente en servidor`() {
        val bad=crear(p=payload(2,3));assertEquals(400,bad.status);assertTrue(json.readTree(bad.contentAsString)["validationErrors"].has("pesoBruto"))
        val good=crear(p=payload(30,2));assertEquals(201,good.status);assertEquals(28.0,json.readTree(good.contentAsString)["pesoNeto"].asDouble())
        assertEquals(400,crear(p=payload()+mapOf("pesoNeto" to 1000)).status)
    }
    @Test fun `RN04 cuenta puntos faltantes y acepta omisiones persistentes`() {
        fun cerrar()=mvc.perform(post("/api/v1/rutas/$ruta/cerrar").header("Authorization","Bearer $supervisor")).andReturn().response
        val conflict=cerrar();assertEquals(409,conflict.status);assertTrue(conflict.contentAsString.contains("2 puntos"))
        assertEquals(201,crear().status)
        val omit=mvc.perform(put("/api/v1/rutas/$ruta/puntos/$otroPunto/omision").header("Authorization","Bearer $operario").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Empresa cerrada\"}")).andReturn().response
        assertEquals(204,omit.status);assertEquals(204,cerrar().status)
    }
    @Test fun `idempotencia evita duplicar y rechaza reutilizar la clave con otros datos`() {
        val solicitud=UUID.randomUUID().toString();val p=payload(solicitud=solicitud)
        val first=crear(p=p);val second=crear(p=p)
        assertEquals(201,first.status);assertEquals(first.contentAsString,second.contentAsString)
        assertEquals(1L,db.queryForObject("SELECT count(*) FROM pesaje WHERE ruta_id=?",Long::class.java,ruta))
        assertEquals(409,crear(p=payload(50,2,solicitud)).status)
    }
    @Test fun `paginacion filtro y recurso inexistente respetan contrato`() {
        val response=mvc.perform(get("/api/v1/puntos").param("pagina","0").param("tamanio","2").param("zona","Norte").header("Authorization","Bearer $operario")).andReturn().response
        assertEquals(200,response.status);val page=json.readTree(response.contentAsString);assertEquals(2,page["contenido"].size());assertEquals(4,page["totalElementos"].asInt());assertFalse(page["ultima"].asBoolean())
        assertEquals(404,mvc.perform(get("/api/v1/puntos/999999/pesajes").header("Authorization","Bearer $operario")).andReturn().response.status)
        assertEquals(400,mvc.perform(get("/api/v1/puntos").param("pagina","-1").header("Authorization","Bearer $operario")).andReturn().response.status)
    }
    @Test fun `rutas de otro usuario no se exponen`() {
        val other=db.queryForObject("INSERT INTO ruta(fecha,estado) VALUES (CURRENT_DATE,'ABIERTA') RETURNING id",Long::class.java)!!
        val data=payload()+mapOf("rutaId" to other)
        assertEquals(403,crear(p=data).status)
        val own=mvc.perform(get("/api/v1/rutas/hoy").header("Authorization","Bearer $operario")).andReturn().response
        assertEquals(ruta,json.readTree(own.contentAsString)["id"].asLong())
    }
}
