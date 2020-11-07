package com.example.duviservicios.ui.view_order

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duviservicios.Adapter.MyOrderAdapter
import com.example.duviservicios.Callback.ILoadOrderCallbackListener
import com.example.duviservicios.Callback.IMyButtonCallBack
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Commun.MySwipeHelper
import com.example.duviservicios.Database.CartDataSource
import com.example.duviservicios.Database.CartDatabase
import com.example.duviservicios.Database.LocalCartDataSource
import com.example.duviservicios.EventBus.CountCartEvent
import com.example.duviservicios.EventBus.MenuItemBack
import com.example.duviservicios.Model.OrderModel
import com.example.duviservicios.Model.ShippingOrderModel
import com.example.duviservicios.R
import com.example.duviservicios.TrackingOrderActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

class ViewOrderFragment : Fragment(),ILoadOrderCallbackListener {

    private var viewOrderModel : ViewOrderModel?=null

    internal lateinit var dialog:AlertDialog

    internal lateinit var recycler_order:RecyclerView

    internal lateinit var listener:ILoadOrderCallbackListener

    lateinit var cartDataSource: CartDataSource
    var compositeDisposable = CompositeDisposable()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewOrderModel = ViewModelProviders.of(this).get(ViewOrderModel::class.java!!)
        val root = inflater.inflate(R.layout.fragmento_view_orders, container,false)

        initViews(root)
        loadOrderFromFirebase()

        viewOrderModel!!.mutableLiveDataOrderList.observe(this, Observer {
            Collections.reverse(it!!)
            val adapter = MyOrderAdapter(context!!,it!!.toMutableList())
            recycler_order!!.adapter = adapter
        })

        return root
    }

    private fun loadOrderFromFirebase() {
        dialog.show()
        val orderList = ArrayList<OrderModel>()

        FirebaseDatabase.getInstance().getReference(Commun.ORDER_REF)
            .orderByChild("userId")

            .equalTo(Commun.currentUser!!.uid!!)
            .limitToLast(100)
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    listener.onLoadOrderFailed(p0.message)
                }

                override fun onDataChange(p0: DataSnapshot) {
                   for (orderSnapShop in p0.children){
                       val order = orderSnapShop.getValue(OrderModel::class.java)
                       order!!.orderNumber = orderSnapShop.key
                       orderList.add(order!!)
                   }
                    listener.onLoadOrderSuccess(orderList)

                }

            })
    }

    private fun initViews(root: View?) {

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        listener = this
        dialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()

        recycler_order = root!!.findViewById(R.id.recycler_order)as RecyclerView
        recycler_order.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context!!)
        recycler_order.layoutManager = layoutManager
        recycler_order.addItemDecoration(DividerItemDecoration(context!!,layoutManager.orientation))

        val swipe = object: MySwipeHelper(context!!,recycler_order!!,250)
        {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(context!!,
                    text = "Cancelar orden",
                    textSize = 25,
                    imageResId = 0,
                    color = Color.parseColor("#FF3C30"),
                    listener = object: IMyButtonCallBack {
                        override fun onClick(pos: Int) {
                            val orderModel =(recycler_order.adapter as MyOrderAdapter).getItemAtPosition(pos)
                            if(orderModel.orderStatus == 0)
                            {
                                val builder = androidx.appcompat.app.AlertDialog.Builder(context!!)
                                builder.setTitle("Cancelar Orden")
                                    .setMessage("Tu realmente quieres cancelar la orden?")
                                    .setNegativeButton("NO"){dialogInterface, i ->
                                        dialogInterface.dismiss()
                                    }
                                    .setPositiveButton("SI"){dialogInterface, i ->

                                        val update_data = HashMap<String,Any>()
                                        update_data.put("orderStatus",-1)
                                        FirebaseDatabase.getInstance()
                                            .getReference(Commun.ORDER_REF)
                                            .child(orderModel.orderNumber!!)
                                            .updateChildren(update_data)
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnSuccessListener {
                                                orderModel.orderStatus =-1
                                                (recycler_order.adapter as MyOrderAdapter).setItemAtPosition(pos,orderModel)
                                                (recycler_order.adapter as MyOrderAdapter).notifyItemChanged(pos)
                                                Toast.makeText(context!!,"Orden cancelada satisfactoriamente",Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                val dialog = builder.create()
                                dialog.show()

                            }
                            else
                            {
                                Toast.makeText(context!!,StringBuilder("Su orden fue cambiada a ")
                                    .append(Commun.convertStatusToText(orderModel.orderStatus))
                                    .append(", tambien puedes cancelarla"),Toast.LENGTH_SHORT).show()
                            }
                        }

                    }))

                buffer.add(MyButton(context!!,
                    text = "Viaje",
                    textSize = 25,
                    imageResId = 0,
                    color = Color.parseColor("#001970"),
                    listener = object: IMyButtonCallBack {
                        override fun onClick(pos: Int) {
                            val orderModel =(recycler_order.adapter as MyOrderAdapter).getItemAtPosition(pos)
                            FirebaseDatabase.getInstance()
                                .getReference(Commun.SHIPPING_ORDER_REF)
                                .child(orderModel.orderNumber!!)
                                .addListenerForSingleValueEvent(object :ValueEventListener
                                {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if(snapshot.exists())
                                        {
                                            Commun.currentShippingOrder = snapshot.getValue(
                                                ShippingOrderModel::class.java)
                                            Commun.currentShippingOrder!!.key = snapshot.key
                                            if(Commun.currentShippingOrder!!.currentLat != -1.0 &&
                                                    Commun.currentShippingOrder!!.currentLng != -1.0)
                                            {
                                                startActivity(Intent(context!!, TrackingOrderActivity::class.java))
                                            } else
                                            {
                                                Toast.makeText(context!!,"\n" +
                                                        "su pedido no ha sido enviado, por favor espere",Toast.LENGTH_SHORT).show()
                                            }
                                        } else
                                        {
                                            Toast.makeText(context!!,"Usted acaba de hacer su pedido, por favor espere el envío",Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(context!!,error.message,Toast.LENGTH_SHORT).show()
                                    }

                                })
                        }

                    }))


                buffer.add(MyButton(context!!,
                    text = "Repetir Orden",
                    textSize = 25,
                    imageResId = 0,
                    color = Color.parseColor("#5d4037"),
                    listener = object: IMyButtonCallBack {
                        override fun onClick(pos: Int) {
                            val orderModel =(recycler_order.adapter as MyOrderAdapter).getItemAtPosition(pos)

                            dialog.show()

                           cartDataSource.cleanCart(Commun.currentUser!!.uid!!)
                               .subscribeOn(Schedulers.io())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe(object :SingleObserver<Int>{
                                   override fun onSuccess(t: Int) {

                                       val cartItems = orderModel.cartItemList!!.toTypedArray()
                                       compositeDisposable.add(
                                           cartDataSource!!.insertOrReplaceAll(*cartItems)
                                               .subscribeOn(Schedulers.io())
                                               .observeOn(AndroidSchedulers.mainThread())
                                               .subscribe({
                                                   dialog.dismiss()
                                                   EventBus.getDefault().postSticky(CountCartEvent(true))
                                                   Toast.makeText(context!!,"Agregados productos correctamente",Toast.LENGTH_SHORT).show()
                                               },{
                                                   t:Throwable? ->
                                                       dialog.dismiss()
                                                   Toast.makeText(context!!,""+t!!.message,Toast.LENGTH_SHORT).show()
                                               })
                                       )
                                   }

                                   override fun onSubscribe(d: Disposable) {

                                   }

                                   override fun onError(e: Throwable) {
                                      dialog.dismiss()
                                       Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()
                                   }

                               })
                        }

                    }))
            }

        }

    }


    override fun onLoadOrderSuccess(orderList: List<OrderModel>) {
        dialog.dismiss()
      viewOrderModel!!.setMutableLiveDataOrderList(orderList)
    }

    override fun onLoadOrderFailed(message: String) {
        dialog.dismiss()
        Toast.makeText(context!!,message,Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        compositeDisposable.clear()
        super.onDestroy()
    }

}