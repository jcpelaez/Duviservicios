package com.example.duviservicios.Callback

import com.example.duviservicios.Database.CartItem
import com.example.duviservicios.Model.CategoryModel

interface ISearchCategoryCallbackListener {
    fun onSearchFound(category:CategoryModel,cartItem: CartItem)
    fun onSearchNoFound(message: String)
}