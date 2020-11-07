package com.example.duviservicios.Callback

import com.example.duviservicios.Model.CategoryModel

interface IcategoryCallBackListener {
    fun onCategoryLoadSuccess(categoriesList:List<CategoryModel>)
    fun onCategoryLoadFailed(messaje:String)
}