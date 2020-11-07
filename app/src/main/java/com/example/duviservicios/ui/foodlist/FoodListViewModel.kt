package com.example.duviservicios.ui.foodlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Model.FoodModel

class FoodListViewModel : ViewModel() {

   private var mutableFoodModelListData:MutableLiveData<List<FoodModel>>?=null

    fun getmutableFoodModelListData():MutableLiveData<List<FoodModel>>{
        if (mutableFoodModelListData == null)
            mutableFoodModelListData = MutableLiveData()
        mutableFoodModelListData!!.value = Commun.categorySeleted!!.foods
        return mutableFoodModelListData!!
    }
}