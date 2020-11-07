package com.example.duviservicios.Callback

import com.example.duviservicios.Model.RestaurantModel

interface IRestaurantCallbackListener {
    fun onRestaurantLoadSuccess(restaurantList:List<RestaurantModel>)
    fun onRestaurantLoadFailed(message:String)
}