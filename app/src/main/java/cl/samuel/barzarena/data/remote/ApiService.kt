package cl.samuel.barzarena.data.remote

import cl.samuel.barzarena.model.UserData
import retrofit2.http.GET

interface ApiService {
    @GET("api.php?endpoint=obtenerDatos")
    suspend fun getDatos(): List<UserData>
}
