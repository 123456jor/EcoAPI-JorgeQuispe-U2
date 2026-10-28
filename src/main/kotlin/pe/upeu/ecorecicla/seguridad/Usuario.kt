package pe.upeu.ecorecicla.seguridad
data class Usuario(val id: Long, val usuario: String, val nombre: String, val clave: String, val rol: Rol)
data class Identidad(val id: Long, val usuario: String, val privilegios: Set<String>) {
    fun tiene(privilegio: String) = privilegio in privilegios
}
