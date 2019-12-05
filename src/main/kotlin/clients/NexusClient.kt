package clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.uptake.omnibus.config
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import services.NexusService
import java.util.concurrent.TimeUnit

object NexusClient {
    var fromRepo: String = ""

    val instance by lazy {
        val nexusClient = OkHttpClient.Builder().addInterceptor {
            val newRequest = it.request()
                    .newBuilder()
                    .header("Accept", "application/json")
                    .build()
            it.proceed(newRequest)
        }
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

        val nexusRetrofit = Retrofit.Builder()
                .baseUrl(config.nexus.apiHandle + "/")
                .addConverterFactory(JacksonConverterFactory.create(ObjectMapper().registerKotlinModule()))
                .client(nexusClient)
                .build()

        nexusRetrofit.create(NexusService::class.java)
    }
}
