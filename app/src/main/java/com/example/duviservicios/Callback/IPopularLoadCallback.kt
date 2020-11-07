package com.example.duviservicios.Callback

import com.example.duviservicios.Model.PopularCategoryModel

interface IPopularLoadCallback {
    fun onPopularLoadSuccess(popularModelList:List<PopularCategoryModel>)
    fun onPopularLoadFailed(messaje:String)
}