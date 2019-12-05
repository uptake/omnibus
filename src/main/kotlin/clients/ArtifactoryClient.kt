package clients

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.uptake.omnibus.config
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import services.ArtifactoryService
import java.util.concurrent.TimeUnit

object ArtifactoryClient {
    val instance by lazy {
        val artifactoryClient = OkHttpClient.Builder().addInterceptor {
            val newRequest = it.request()
                    .newBuilder()
                    .header("Accept", "application/json")
                    .header("X-JFrog-Art-Api", config.artifactory.apiKey)
                    .build()
            it.proceed(newRequest)
        }
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

        val artifactoryRetrofit = Retrofit.Builder()
                .baseUrl(config.artifactory.host)
                .addConverterFactory(JacksonConverterFactory.create(ObjectMapper().registerKotlinModule()))
                .client(artifactoryClient)
                .build()

        artifactoryRetrofit.create(ArtifactoryService::class.java)
    }

    val sourceInstance by lazy {
        val artifactoryClient = OkHttpClient.Builder().addInterceptor {
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

        val artifactoryRetrofit = Retrofit.Builder()
                .baseUrl(config.artifactory.sourceHost)
                .addConverterFactory(JacksonConverterFactory.create(ObjectMapper().registerKotlinModule()))
                .client(artifactoryClient)
                .build()

        artifactoryRetrofit.create(ArtifactoryService::class.java)
    }
}
