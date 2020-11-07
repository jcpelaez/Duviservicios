package com.example.duviservicios.Callback

import com.example.duviservicios.Model.OrderModel


interface ILoadTimeFromFirebaseCallback {
    fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs:Long)
    fun onLoadTimeFailed(messaje:String)
}