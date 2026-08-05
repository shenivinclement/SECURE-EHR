package com.secureehr.app.data.blockchain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object IPFSService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var pinataJwt: String = ""

    fun configure(jwt: String) {
        pinataJwt = jwt
    }

    /**
     * Upload [fileBytes] to IPFS via Pinata.
     * Returns the IPFS CID (e.g. "Qm...").
     * Throws if JWT is not configured or upload fails.
     */
    suspend fun uploadToIPFS(fileBytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            if (pinataJwt.isBlank()) throw IllegalStateException("Pinata JWT not configured")

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    fileBytes.toRequestBody("application/octet-stream".toMediaType())
                )
                .addFormDataPart(
                    "pinataMetadata",
                    null,
                    """{"name":"$fileName"}""".toRequestBody("application/json".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.pinata.cloud/pinning/pinFileToIPFS")
                .addHeader("Authorization", "Bearer $pinataJwt")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty Pinata response")
            if (!response.isSuccessful) throw Exception("Pinata error ${response.code}: $body")
            JSONObject(body).getString("IpfsHash")
        }

    fun getIPFSUrl(cid: String): String = "https://gateway.pinata.cloud/ipfs/$cid"
}
