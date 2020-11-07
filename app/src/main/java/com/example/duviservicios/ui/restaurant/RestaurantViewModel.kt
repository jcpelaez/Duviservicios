package com.example.duviservicios.ui.restaurant

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.duviservicios.Callback.IRestaurantCallbackListener
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Model.RestaurantModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RestaurantViewModel : ViewModel(), IRestaurantCallbackListener {

    private var restaurantListMutable : MutableLiveData<List<RestaurantModel>>?=null
    private var messajeError:MutableLiveData<String> = MutableLiveData()
    private val restaurantCallBackListener:IRestaurantCallbackListener

    init {
        restaurantCallBackListener = this
    }


    fun getRestaurantList():MutableLiveData<List<RestaurantModel>>{
        if(restaurantListMutable == null)
        {
            restaurantListMutable = MutableLiveData()
            loadRestaurantFromFirebase()
        }
        return restaurantListMutable!!
    }

    private fun loadRestaurantFromFirebase() {
        val tempList = ArrayList<RestaurantModel>()
        val restaurantRef = FirebaseDatabase.getInstance().getReference(Commun.RESTAURANT_REF)
        restaurantRef.addListenerForSingleValueEvent(object: ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError) {
                restaurantCallBackListener.onRestaurantLoadFailed((p0.message!!))
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (p0.exists()) {
                    for (itemSnapShot in p0!!.children) {
                        val model = itemSnapShot.getValue<RestaurantModel>(RestaurantModel::class.java)
                        model!!.uid = itemSnapShot.key!!
                        tempList.add(model!!)
                    }
                    if(tempList.size > 0)
                        restaurantCallBackListener.onRestaurantLoadSuccess(tempList)
                    else
                        restaurantCallBackListener.onRestaurantLoadFailed("Listado de restaurante vacio")
                } else
                    restaurantCallBackListener.onRestaurantLoadFailed("El listado del restaurante no existe")
            }
        })
    }

    fun getMessajeError():MutableLiveData<String>
    {
        return messajeError
    }

    override fun onRestaurantLoadSuccess(restaurantList: List<RestaurantModel>) {
       restaurantListMutable!!.value = restaurantList
    }

    override fun onRestaurantLoadFailed(message: String) {
        messajeError.value = message
    }
}
