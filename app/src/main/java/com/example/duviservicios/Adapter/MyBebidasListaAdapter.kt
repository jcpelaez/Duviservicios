package com.example.duviservicios.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.duviservicios.Callback.IRecyclerItemClickListener
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Database.CartDataSource
import com.example.duviservicios.Database.CartDatabase
import com.example.duviservicios.Database.CartItem
import com.example.duviservicios.Database.LocalCartDataSource
import com.example.duviservicios.EventBus.BebidasItemClick
import com.example.duviservicios.EventBus.CountCartEvent
import com.example.duviservicios.Model.BebidasModel
import com.example.duviservicios.R
import io.reactivex.Scheduler
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class MyBebidasListaAdapter (internal var context: Context,
                             internal var bebidasList: List<BebidasModel>):
    RecyclerView.Adapter<MyBebidasListaAdapter.MyViewHolder>(){

    private val compositeDisposable : CompositeDisposable
    private val cartDataSource : CartDataSource

    init {
       compositeDisposable = CompositeDisposable()
        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context).cartDAO())
    }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            Glide.with(context).load(bebidasList.get(position).image).into(holder.img_food_image!!)
            holder.txt_food_name!!.setText(bebidasList.get(position).name)
            holder.txt_food_price!!.setText(StringBuilder("$").append(bebidasList.get(position).price.toString()))

            holder.setListener(object : IRecyclerItemClickListener {
                override fun onItemClick(view: View, pos: Int) {
                    Commun.bebidasSelected = bebidasList.get(pos)
                    Commun.bebidasSelected!!.key = pos.toString()
                    EventBus.getDefault().postSticky(BebidasItemClick(true, bebidasList.get(pos)))
                }
            })


           /* holder.img_cart!!.setOnClickListener {
                //TODO//
                var cartItem = CartItem()
                cartItem.uid = Commun.currentUser!!.uid
               // cartItem.uid = "396695517652654"
                cartItem.userPhone = Commun.currentUser!!.phone
                //cartItem.userPhone = "+84988353682"


                cartItem.foodId = foodList.get(position).id!!
                cartItem.foodName = foodList.get(position).name!!
                cartItem.foodImage = foodList.get(position).image!!
                cartItem.foodPrice = foodList.get(position).price!!.toDouble()
                cartItem.foodQuantity = 1
                cartItem.foodExtraPrice = 0.0
                cartItem.foodAddon = "Default"
                cartItem.foodSize = "Default"

                cartDataSource.getItemWithAllOptionsInCart(
                   //TODO
                     //"396695517652654",
                    Commun.currentUser!!.uid!!,
                    cartItem.foodId,
                    cartItem.foodSize!!,
                    cartItem.foodAddon!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<CartItem> {
                        override fun onSuccess(cartItemFromDB: CartItem) {

                            if(cartItemFromDB.equals(cartItem))
                            {
                                cartItemFromDB.foodExtraPrice = cartItem.foodExtraPrice
                                cartItemFromDB.foodAddon = cartItem.foodAddon
                                cartItemFromDB.foodSize = cartItem.foodSize
                                cartItemFromDB.foodQuantity = cartItemFromDB.foodQuantity + cartItem.foodQuantity

                                cartDataSource.updateCart(cartItemFromDB)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : SingleObserver<Int> {
                                        override fun onSuccess(t: Int) {
                                            Toast.makeText(context,"Canasta actualizada con éxito",Toast.LENGTH_SHORT).show()
                                            EventBus.getDefault().postSticky(CountCartEvent(true))
                                        }

                                        override fun onSubscribe(d: Disposable) {

                                        }

                                        override fun onError(e: Throwable) {
                                            Toast.makeText(context,"[UPDATE CART]"+e.message,Toast.LENGTH_SHORT).show()
                                        }

                                    })
                            } else
                            {
                                compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        Toast.makeText(
                                            context,
                                            "Producto agregado a tu canasta",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                    }, { t: Throwable? ->
                                        Toast.makeText(
                                            context,
                                            "[INSERT CART]" + t!!.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                                )
                            }
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            if(e.message!!.contains("empty")) {
                                compositeDisposable.add(cartDataSource.insertOrReplaceAll(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe({
                                        Toast.makeText(
                                            context,
                                            "Producto agregado a tu canasta",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                    }, { t: Throwable? ->
                                        Toast.makeText(
                                            context,
                                            "[INSERT CART]" + t!!.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    })
                                )
                            } else
                                Toast.makeText(context,"[CART ERROR]"+e.message,Toast.LENGTH_SHORT).show()
                        }
                    })

            }*/
        }

            fun onStop(){
              if (compositeDisposable != null)
              compositeDisposable.clear()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyBebidasListaAdapter.MyViewHolder {
            return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_bebidas_item,parent,false))
        }

        override fun getItemCount(): Int {
            return bebidasList.size
        }

        inner class MyViewHolder(itemView : View) : RecyclerView.ViewHolder (itemView),
            View.OnClickListener {
            override fun onClick(view: View?) {
                listener!!.onItemClick(view!!,adapterPosition)
            }

            var txt_food_name: TextView?=null
            var txt_food_price: TextView?=null

            var img_food_image: ImageView?=null
           // var img_fav: ImageView?=null
           // var img_cart: ImageView?=null

            internal var listener: IRecyclerItemClickListener?=null

            fun setListener(listener: IRecyclerItemClickListener)
            {
                this.listener = listener
            }

            init {
                txt_food_name = itemView.findViewById(R.id.txt_food_name) as TextView
                txt_food_price = itemView.findViewById(R.id.txt_food_price) as TextView
                img_food_image = itemView.findViewById(R.id.img_food_image) as ImageView
                //img_fav = itemView.findViewById(R.id.img_fav) as ImageView
               // img_cart = itemView.findViewById(R.id.img_quick_cart) as ImageView

                itemView.setOnClickListener(this)
            }
        }
}