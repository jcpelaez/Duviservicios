package com.example.duviservicios.Callback

import com.example.duviservicios.Model.OrderModel

interface ILoadOrderCallbackListener {
    fun onLoadOrderSuccess(orderList:List<OrderModel>)
    fun onLoadOrderFailed(message:String)
}