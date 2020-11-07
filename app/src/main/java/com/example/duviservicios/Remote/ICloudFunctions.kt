package com.example.duviservicios.Remote

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

interface ICloudFunctions {
    @GET("getCustomToken")
    fun getCustomToken(@Query("access_token")accessToken:String): Observable<ResponseBody>

}