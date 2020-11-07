package com.example.duviservicios.ui.menu

import android.renderscript.Sampler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.duviservicios.Callback.IcategoryCallBackListener
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Model.CategoryModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuViewModel : ViewModel(), IcategoryCallBackListener {
    override fun onCategoryLoadSuccess(categoriesList: List<CategoryModel>) {
        categoriesListMutable!!.value = categoriesList
    }

    override fun onCategoryLoadFailed(messaje: String) {
        messajeError.value = messaje
    }

    private var categoriesListMutable : MutableLiveData<List<CategoryModel>>?=null
    private var messajeError:MutableLiveData<String> = MutableLiveData()
    private val categoryCallBackListener:IcategoryCallBackListener

    init {
        categoryCallBackListener = this
    }

    fun getCategoryList():MutableLiveData<List<CategoryModel>>{
        if(categoriesListMutable == null)
        {
            categoriesListMutable = MutableLiveData()
            loadCategory()
        }
        return categoriesListMutable!!
    }

    fun getMessajeError():MutableLiveData<String>
    {
        return messajeError
    }

    fun loadCategory() {
       val tempList = ArrayList<CategoryModel>()
        val categoryRef = FirebaseDatabase.getInstance().getReference(Commun.RESTAURANT_REF)
            .child(Commun.currentRestaurant!!.uid)
            .child(Commun.CATEGORY_REF)
        categoryRef.addListenerForSingleValueEvent(object: ValueEventListener
        {
            override fun onCancelled(p0: DatabaseError) {
                categoryCallBackListener.onCategoryLoadFailed((p0.message!!))
            }

            override fun onDataChange(p0: DataSnapshot) {
                for (itemSnapShot in p0!!.children)
                {
                    val model = itemSnapShot.getValue<CategoryModel>(CategoryModel::class.java)
                    model!!.menu_id = itemSnapShot.key
                    tempList.add(model!!)
                }
                categoryCallBackListener.onCategoryLoadSuccess(tempList)
            }
        })
    }


}