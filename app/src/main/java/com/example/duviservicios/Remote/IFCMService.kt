package com.example.duviservicios.Remote

import com.example.duviservicios.Model.FCMResponse
import com.example.duviservicios.Model.FCMSendData
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IFCMService {
    @Headers(
        "Content-Type:application/json",
        "Authorization:key=AAAA4UsK3SQ:APA91bG6tP6VbltR589afUMit15hKm6Mo9s0_4wYbJs0xg8-AyZqR0UptNX93OuwDXvqdN-1S8eiQ2wqzbkCVrlOPl6NFhCy_gO810yAKLOT55a4aTBHnNfL6rz_qEFT_MLrfpv4nF06"
    )
    @POST("fcm/send")
    fun sendNotification(@Body body: FCMSendData): Observable<FCMResponse>

}