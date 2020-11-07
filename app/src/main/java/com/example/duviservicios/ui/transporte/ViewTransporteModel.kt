package com.example.duviservicios.ui.transporte

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.duviservicios.Model.OrderModel

class ViewTransporteModel: ViewModel() {
    val mutableLiveDataOrderList: MutableLiveData<List<OrderModel>>
    init {
        mutableLiveDataOrderList = MutableLiveData()
    }

    fun setMutableLiveDataOrderList(orderList: List<OrderModel>)
    {
        mutableLiveDataOrderList.value = orderList
    }

}

