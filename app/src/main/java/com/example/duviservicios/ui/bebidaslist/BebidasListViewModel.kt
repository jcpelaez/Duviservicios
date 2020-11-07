package com.example.duviservicios.ui.bebidaslist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Model.BebidasModel

class BebidasListViewModel : ViewModel() {

   private var mutableBebidasModelListData:MutableLiveData<List<BebidasModel>>?=null

    fun getmutableFoodModelListData():MutableLiveData<List<BebidasModel>>{
        if (mutableBebidasModelListData == null)
            mutableBebidasModelListData = MutableLiveData()
        mutableBebidasModelListData!!.value != null
        return mutableBebidasModelListData!!
    }
}