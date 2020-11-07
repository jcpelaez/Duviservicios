package com.example.duviservicios.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.duviservicios.Callback.IBestDealLoadCallback
import com.example.duviservicios.Callback.IPopularLoadCallback
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Model.BestDealModel
import com.example.duviservicios.Model.PopularCategoryModel
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel(), IPopularLoadCallback, IBestDealLoadCallback {
    override fun onBeastDealLoadSuccess(bestDealList: List<BestDealModel>) {
        bestDealListMutableLiveData!!.value = bestDealList
    }

    override fun onBeastDealLoadFailed(messaje: String) {
        messageError.value = messaje
    }

    override fun onPopularLoadSuccess(popularModelList: List<PopularCategoryModel>) {
        popularListMutableLiveData!!.value = popularModelList
    }

    override fun onPopularLoadFailed(messaje: String) {
        messageError.value = messaje
    }

    private var popularListMutableLiveData: MutableLiveData<List<PopularCategoryModel>>? = null
    private var bestDealListMutableLiveData: MutableLiveData<List<BestDealModel>>? = null
    private lateinit var messageError: MutableLiveData<String>
    private var popularLoadCallbackListener: IPopularLoadCallback
    private var bestDealCallbackListener: IBestDealLoadCallback


    val bestDealList:LiveData<List<BestDealModel>>
        get() {
            if (bestDealListMutableLiveData == null){

                bestDealListMutableLiveData = MutableLiveData()
                messageError = MutableLiveData()
                loadBestDealList()
            }
            return bestDealListMutableLiveData!!
        }

    private fun loadBestDealList() {
        val tempList = ArrayList<BestDealModel>()
        val bestDealRef = FirebaseDatabase.getInstance().getReference(Commun.BEST_DEAL_REF)
        bestDealRef.addListenerForSingleValueEvent(object:ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                bestDealCallbackListener.onBeastDealLoadFailed((p0.message!!))
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (itemSnapshot in p0!!.children)
                {
                    val model = itemSnapshot.getValue<BestDealModel>(BestDealModel::class.java)
                    tempList.add(model!!)
                }
                bestDealCallbackListener.onBeastDealLoadSuccess(tempList)
            }
        })
    }

    val popularList:LiveData<List<PopularCategoryModel>>
        get() {
            if(popularListMutableLiveData == null)
            {
                popularListMutableLiveData = MutableLiveData()
                messageError = MutableLiveData()
                LoadPopularList()
            }
            return popularListMutableLiveData!!
        }

    private fun LoadPopularList() {
        val tempList = ArrayList<PopularCategoryModel>()
        val popularRef = FirebaseDatabase.getInstance().getReference(Commun.POPULAR_REF)
        popularRef.addListenerForSingleValueEvent(object:ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                popularLoadCallbackListener.onPopularLoadFailed((p0.message!!))
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (itemSnapshot in p0!!.children)
                {
                    val model = itemSnapshot.getValue<PopularCategoryModel>(PopularCategoryModel::class.java)
                    tempList.add(model!!)
                }
                popularLoadCallbackListener.onPopularLoadSuccess(tempList)
            }
        })

    }

    init {
        popularLoadCallbackListener = this
        bestDealCallbackListener = this
    }
}