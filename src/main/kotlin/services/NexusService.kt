package services

import clients.NexusClient
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import models.nexus.NexusStorage
import models.nexus.NexusFileInfoWrapper
import okhttp3.ResponseBody

interface NexusService {
    @GET("repositories/{repository}/content/{packagePath}")
    fun getRepoContent(@Path("packagePath") packagePath: String,
                       @Path("repository") repository: String = NexusClient.fromRepo): Call<NexusStorage>

    @GET("repositories/{repository}/content/{filePath}")
    fun getFileContent(@Path("filePath") filePath: String,
                       @Path("repository") repository: String = NexusClient.fromRepo): Call<ResponseBody>

    @GET("repositories/{repository}/content/{filePath}?describe=info")
    fun getFileInfo(@Path("filePath") filePath: String,
                    @Path("repository") repository: String = NexusClient.fromRepo): Call<NexusFileInfoWrapper>
}
