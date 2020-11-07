package com.example.duviservicios.Callback

import com.example.duviservicios.Model.BestDealModel
import com.example.duviservicios.Model.PopularCategoryModel

interface IBestDealLoadCallback {
    fun onBeastDealLoadSuccess(bestDealList:List<BestDealModel>)
    fun onBeastDealLoadFailed(messaje:String)
}