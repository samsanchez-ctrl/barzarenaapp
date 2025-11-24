package cl.samuel.barzarena.data.remote

import cl.samuel.barzarena.model.UserData
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api.php?endpoint=obtenerDatos")
    suspend fun getDatos(@Query("endpoint") endpoint: String): List<UserData>
}
