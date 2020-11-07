package com.example.duviservicios.ui.view_order

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.duviservicios.Model.OrderModel

class ViewOrderModel: ViewModel() {
    val mutableLiveDataOrderList: MutableLiveData<List<OrderModel>>
    init {
        mutableLiveDataOrderList = MutableLiveData()
    }

    fun setMutableLiveDataOrderList(orderList: List<OrderModel>)
    {
        mutableLiveDataOrderList.value = orderList
    }

}

