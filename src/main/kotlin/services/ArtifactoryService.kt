package services

import models.GenericContentItem
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Header
import retrofit2.http.Body

interface ArtifactoryService {
    @GET("api/storage/{packagePath}")
    fun getArtifactInfo(@Path("packagePath") packagePath: String): Call<GenericContentItem>

    @PUT("{packagePath}")
    fun deployArtifact(@Path("packagePath") packagePath: String, @Header("X-Checksum-Sha1") sha1: String,
                        @Body body: RequestBody): Call<ResponseBody>

    @GET("/{packagePath}")
    fun getFileContent(@Path("packagePath") packagePath: String): Call<ResponseBody>
}
