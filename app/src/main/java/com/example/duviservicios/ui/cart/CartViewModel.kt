package com.example.duviservicios.ui.cart

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Database.CartDataSource
import com.example.duviservicios.Database.CartDatabase
import com.example.duviservicios.Database.CartItem
import com.example.duviservicios.Database.LocalCartDataSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class CartViewModel : ViewModel() {

   private val compositeDisposable:CompositeDisposable
    private var cartDataSource:CartDataSource?=null
    private var mutableLiveDataCartItem:MutableLiveData<List<CartItem>>?=null


    init {
        compositeDisposable = CompositeDisposable()
    }

    fun InitCartDataSource(context: Context)
    {
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDAO())
    }

    fun getMutableLiveCartItem():MutableLiveData<List<CartItem>>{
        if(mutableLiveDataCartItem == null)
            mutableLiveDataCartItem = MutableLiveData()
        getCartItems()
        return mutableLiveDataCartItem!!
    }

    private fun getCartItems()
    {
        compositeDisposable.addAll(cartDataSource!!.getAllCart(Commun.currentUser!!.uid!!)

            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({cartItem ->
                mutableLiveDataCartItem!!.value = cartItem

            },{t: Throwable? -> mutableLiveDataCartItem!!.value = null  }))
    }

    fun onStop()
    {
        compositeDisposable.clear()
    }
}