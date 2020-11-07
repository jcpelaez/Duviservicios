package com.example.duviservicios.ui.cart

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duviservicios.Adapter.MyCartAdapter
import com.example.duviservicios.Callback.ILoadTimeFromFirebaseCallback
import com.example.duviservicios.Callback.IMyButtonCallBack
import com.example.duviservicios.Callback.ISearchCategoryCallbackListener
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Commun.MySwipeHelper
import com.example.duviservicios.Database.CartDataSource
import com.example.duviservicios.Database.CartDatabase
import com.example.duviservicios.Database.CartItem
import com.example.duviservicios.Database.LocalCartDataSource
import com.example.duviservicios.EventBus.CountCartEvent
import com.example.duviservicios.EventBus.HideFABCart
import com.example.duviservicios.EventBus.MenuItemBack
import com.example.duviservicios.EventBus.UpdateItemInCart
import com.example.duviservicios.Model.*
import com.example.duviservicios.R
import com.example.duviservicios.Remote.IFCMService
import com.example.duviservicios.Remote.RetrofitCloudClient
import com.example.duviservicios.Remote.RetrofitFCMClient
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CartFragment : Fragment(), ILoadTimeFromFirebaseCallback, ISearchCategoryCallbackListener,
    TextWatcher {

    override fun onLoadTimeSuccess(order: OrderModel, estimatedTimeMs: Long) {
        order.createDate = (estimatedTimeMs)
        order.orderStatus = 0
        writeorderToFirebase(order)
    }

    override fun onLoadTimeFailed(messaje: String) {
        Toast.makeText(context!!,messaje,Toast.LENGTH_SHORT).show()
    }

    private lateinit var addonBottomSheetDialog: BottomSheetDialog
    private var chip_group_user_seleted_addon: ChipGroup?=null
    private var chip_group_addon : ChipGroup?=null
    private var edt_search_addon : EditText?=null

    lateinit var searchCategoryCallbackListener: ISearchCategoryCallbackListener

    private var cartDataSource:CartDataSource?=null
    private var compositeDisposable:CompositeDisposable = CompositeDisposable()
    private var recyclerViewState:Parcelable?=null
    private lateinit var cartViewModel: CartViewModel
    private lateinit var btn_place_order:Button

    var txt_empty_cart:TextView?=null
    var txt_total_price:TextView?=null
    var group_place_holder:CardView?=null
    var recycler_cart:RecyclerView?=null
    var adapter:MyCartAdapter?=null

    private lateinit var locationRequest:LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation:Location

    internal var address:String=""
    internal var comment:String=""

    lateinit var ifcmService: IFCMService
    lateinit var listener:ILoadTimeFromFirebaseCallback

    override fun onResume() {
        super.onResume()
        calculateTotalPrice()
        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,
                Looper.getMainLooper())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        EventBus.getDefault().postSticky(HideFABCart(true))

        cartViewModel =
            ViewModelProviders.of(this).get(CartViewModel::class.java)

        cartViewModel.InitCartDataSource(context!!)

        val root = inflater.inflate(R.layout.fragment_cart, container, false)
        initLocation()

        cartViewModel.getMutableLiveCartItem().observe(this, Observer {
            if(it == null || it.isEmpty())
            {
                recycler_cart!!.visibility = View.GONE
                group_place_holder!!.visibility = View.GONE
                txt_empty_cart!!.visibility = View.VISIBLE
            }
            else
            {
                recycler_cart!!.visibility = View.VISIBLE
                group_place_holder!!.visibility = View.VISIBLE
                txt_empty_cart!!.visibility = View.GONE

                adapter = MyCartAdapter(context!!,it)
                recycler_cart!!.adapter = adapter
            }
        })

        initViews(root)
        return root
    }

    private fun initLocation() {
        builLocationRequest()
        builLocationCallBack()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        fusedLocationProviderClient!!.requestLocationUpdates(locationRequest,locationCallback,Looper.getMainLooper())
    }

    private fun builLocationCallBack() {
        locationCallback = object :LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                currentLocation = p0!!.lastLocation
            }
        }
    }

    private fun builLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(5000)
        locationRequest.setFastestInterval(3000)
        locationRequest.setSmallestDisplacement(10f)
    }

    private fun initViews(root : View) {

        searchCategoryCallbackListener = this

        setHasOptionsMenu(true)

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService::class.java)

        listener = this

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        addonBottomSheetDialog = BottomSheetDialog(context!!,R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display,null)
        chip_group_addon = layout_user_selected_addon.findViewById(R.id.chip_group_addon) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search) as EditText
        addonBottomSheetDialog.setContentView(layout_user_selected_addon)

        addonBottomSheetDialog.setOnDismissListener{ dialogInterface ->
            displayUserSelectedAddon(chip_group_user_seleted_addon)
            calculateTotalPrice()
        }

        recycler_cart = root.findViewById(R.id.recycler_cart)as RecyclerView
        recycler_cart!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        recycler_cart!!.layoutManager = layoutManager
        recycler_cart!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))

        val swipe = object:MySwipeHelper(context!!,recycler_cart!!,200)
        {
            override fun instantiateMyButton(
                viewHolder: RecyclerView.ViewHolder,
                buffer: MutableList<MyButton>
            ) {
                buffer.add(MyButton(context!!,
                    text = "Eliminar",
                    textSize = 30,
                    imageResId = 0,
                    color = Color.parseColor("#FF3C30"),
                    listener = object:IMyButtonCallBack{
                        override fun onClick(pos: Int) {
                           val deleteItem = adapter!!.getItemAtPosition(pos)
                            cartDataSource!!.deleteCart(deleteItem)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(object : SingleObserver<Int>{
                                    override fun onSuccess(t: Int) {
                                        adapter!!.notifyItemRemoved(pos)
                                        sumCart()
                                        EventBus.getDefault().postSticky(CountCartEvent(true))
                                        Toast.makeText(context,"Producto eliminado satisfactoriamente",Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onSubscribe(d: Disposable) {

                                    }

                                    override fun onError(e: Throwable) {
                                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                                    }

                                })
                        }}))

                buffer.add(MyButton(context!!,
                    text = "Actualizar",
                    textSize = 30,
                    imageResId = 0,
                    color = Color.parseColor("#5d4037"),
                    listener = object:IMyButtonCallBack{
                        override fun onClick(pos: Int)
                        {
                            val cartItem = adapter!!.getItemAtPosition(pos)
                            FirebaseDatabase.getInstance()
                                .getReference(Commun.CATEGORY_REF)
                                .child(cartItem.categoryId)
                                .addListenerForSingleValueEvent(object :ValueEventListener{
                                    override fun onCancelled(error: DatabaseError) {
                                        searchCategoryCallbackListener.onSearchNoFound(error.message)
                                    }

                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if(snapshot.exists())
                                        {
                                            val categoryModel = snapshot.getValue(CategoryModel::class.java)
                                            searchCategoryCallbackListener!!.onSearchFound(categoryModel!!,cartItem)
                                        }
                                    }

                                })
                        }
                    }))
            }

        }

        txt_empty_cart = root.findViewById(R.id.txt_empty_cart)as TextView
        txt_total_price = root.findViewById((R.id.txt_total_price))as TextView
        group_place_holder = root.findViewById(R.id.group_place_holder)as CardView

        btn_place_order = root.findViewById(R.id.btn_place_order)as Button

        //Evento
        btn_place_order.setOnClickListener{
            val builder = AlertDialog.Builder(context!!)
            builder.setTitle("Un paso más!")

            val view= LayoutInflater.from(context).inflate(R.layout.layout_place_order,null)

            val edt_address = view.findViewById<View>(R.id.edt_address) as EditText
            val edt_comment = view.findViewById<View>(R.id.edt_comment) as EditText
            val txt_address = view.findViewById<View>(R.id.txt_address_detail) as TextView
            val rdi_ship_to_this_address = view.findViewById<View>(R.id.rdi_ship_this_address) as RadioButton
            val rdi_cod = view.findViewById<View>(R.id.rdi_cod) as RadioButton
            val rdi_other_address = view.findViewById<View>(R.id.rdi_other_address) as RadioButton

            rdi_other_address.setOnCheckedChangeListener{compoundButton, b ->
                if(b)
                {
                    edt_address.setText("")
                    edt_address.setHint("Ingrese su dirección")
                    txt_address.visibility = View.GONE
                }
            }

            rdi_ship_to_this_address.setOnCheckedChangeListener{compoundButton, b ->
                if(b)
                {
                    fusedLocationProviderClient!!.lastLocation
                        .addOnFailureListener {e ->
                            txt_address.visibility = View.GONE
                            Toast.makeText(context!!,""+ e.message, Toast.LENGTH_SHORT).show()
                        }
                        .addOnCompleteListener {
                            task ->
                            val coordinates = StringBuilder()
                                .append(task.result!!.latitude)
                                .append("/")
                                .append(task.result!!.longitude)
                                .toString()

                            val singleAddress = Single.just(getAddresFromLarLong(task.result!!.latitude,
                                task.result!!.longitude))

                            val disposable = singleAddress.subscribeWith(object:DisposableSingleObserver<String>(){
                                override fun onSuccess(t: String) {
                                    edt_address.setText(coordinates)
                                    txt_address.visibility= View.VISIBLE
                                    txt_address.setText(t)
                                }

                                override fun onError(e: Throwable) {
                                    edt_address.setText(coordinates)
                                    txt_address.visibility= View.VISIBLE
                                    txt_address.setText(e.message!!)
                                }
                            })
                        }
                }
            }

            builder.setView(view)
            builder.setNegativeButton("NO",{
                dialogInterface, i -> dialogInterface.dismiss()
            })
                .setPositiveButton("SI",{dialogInterface, i ->
                    paymentCOD(edt_address.text.toString(),edt_comment.text.toString())
                })

            val dialog = builder.create()
            dialog.show()

        }

    }

    private fun displayUserSelectedAddon(chipGroupUserSeletedAddon: ChipGroup?) {
        if(Commun.foodSelected!!.userSelectedAddon != null && Commun.foodSelected!!.userSelectedAddon!!.size > 0)
        {
            chipGroupUserSeletedAddon!!.removeAllViews()
            for(addonModel in Commun.foodSelected!!.userSelectedAddon!!)
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("+($")
                    .append(addonModel.price).append(")")
                chip.setOnCheckedChangeListener{ compoundButton, b ->
                    if(b)
                        if(Commun.foodSelected!!.userSelectedAddon == null) Commun.foodSelected!!.userSelectedAddon = ArrayList()
                    Commun.foodSelected!!.userSelectedAddon!!.add(addonModel)
                }
                chipGroupUserSeletedAddon.addView(chip)
            }
        }
        chipGroupUserSeletedAddon!!.removeAllViews()
    }

    private fun paymentCOD(address: String, comment: String) {

        compositeDisposable.add(cartDataSource!!.getAllCart(Commun.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ cartItemList ->

                cartDataSource!!.sumPrice(Commun.currentUser!!.uid!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : SingleObserver<Double>{
                        override fun onSuccess(totalPrice: Double) {

                            val finalPrice = totalPrice
                            val order = OrderModel()

                            order.userId = Commun.currentUser!!.uid!!

                            order.userName = Commun.currentUser!!.name!!
                            order.userPhone = Commun.currentUser!!.phone!!
                            order.shippingAddress = address
                            order.comment = comment

                            if (currentLocation != null)
                            {
                                order.lat = currentLocation!!.latitude
                                order.lng = currentLocation!!.longitude
                            }

                            order.cartItemList = cartItemList
                            order.totalPayment = totalPrice
                            order.finalPayment = finalPrice
                            order.discount = 0
                            order.isCod = true
                            order.transactionId = "Pago en efectivo"

                         synLocalTimeWithServerTime(order)
                        }

                        override fun onSubscribe(d: Disposable) {

                        }

                        override fun onError(e: Throwable) {
                            if(!e.message!!.contains("Query returned empty"))
                                Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                        }

                    })

            },{ throwable -> Toast.makeText(context!!,""+throwable.message,Toast.LENGTH_SHORT).show() }))
    }

    private fun synLocalTimeWithServerTime(order: OrderModel) {
        val offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
        offsetRef.addListenerForSingleValueEvent(object :ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                listener.onLoadTimeFailed(p0.message)
            }

            override fun onDataChange(p0: DataSnapshot) {
               val offset = p0.getValue(Long::class.java)
                val estimatedServerTimeInMs = System.currentTimeMillis() + offset!!
                val sdf = SimpleDateFormat("MMM dd yyyy, HH:mm")
                val date = Date(estimatedServerTimeInMs)
                Log.d("Zerox",""+sdf.format(date))
                listener.onLoadTimeSuccess(order,estimatedServerTimeInMs)
            }

        })
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    private fun writeorderToFirebase(order: OrderModel) {

        FirebaseDatabase.getInstance()
            .getReference(Commun.ORDER_REF)
            .child(Commun.createOrderNumber())
            .setValue(order)
            .addOnFailureListener {e -> Toast.makeText(context!!,""+e.message,Toast.LENGTH_SHORT).show()}
            .addOnCompleteListener { task ->

               if(task.isSuccessful)
                {
                         cartDataSource!!.cleanCart(Commun.currentUser!!.uid!!)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : SingleObserver<Int>{
                            override fun onSuccess(t: Int) {

                                val dataSend = HashMap<String,String>()
                                dataSend.put(Commun.NOTI_TITLE,"Nueva Orden")
                                dataSend.put(Commun.NOTI_CONTENT,"Tu tienes una nueva orden "+ Commun.currentUser!!.phone)

                                val sendData = FCMSendData(Commun.getNewOrderTopic(),dataSend)

                                compositeDisposable.add(
                                    ifcmService.sendNotification(sendData)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe ({t: FCMResponse? ->
                                            if(t!!.success != 0)
                                                Toast.makeText(context!!,"Orden generada exitosamente",Toast.LENGTH_SHORT).show()
                                        },{t: Throwable? ->
                                            Toast.makeText(context!!,"Orden fue enviada pero la notificación fallo",Toast.LENGTH_SHORT).show()
                                        }))

                            }

                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(context!!,""+e.message,Toast.LENGTH_SHORT).show()
                            }
                        })
                }
            }
    }

    private fun getAddresFromLarLong(latitude: Double, longitude: Double): String {
        val geoCoder = Geocoder(context!!, Locale.getDefault())
        var result:String?=null
        try {
            val addressList = geoCoder.getFromLocation(latitude,longitude,1)
            if(addressList != null && addressList.size > 0)
            {
                val address = addressList[0]
                val sb = StringBuilder(address.getAddressLine(0))
                result = sb.toString()
            }
            else
                result = "Dirección no encontrada!"
            return result
        }catch (e:IOException)
        {
            return e.message!!
        }
    }

    private fun sumCart() {
        cartDataSource!!.sumPrice(Commun.currentUser!!.uid!!)

            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object :SingleObserver<Double>{
                override fun onSuccess(t: Double) {
                    txt_total_price!!.text = StringBuilder("Total: $")
                        .append(t)
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if(!e.message!!.contains("Query returned empty"))
                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                }

            })
    }

    override fun onStart() {
        super.onStart()
        if(!EventBus.getDefault().isRegistered(this))
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        cartViewModel!!.onStop()
        compositeDisposable.clear()
        EventBus.getDefault().postSticky(HideFABCart(false))
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this)
        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    @Subscribe (sticky = true,threadMode = ThreadMode.MAIN)
    fun onUpdateItemCart(event:UpdateItemInCart)
    {
        if(event.cartItem != null)
        {
            recyclerViewState = recycler_cart!!.layoutManager!!.onSaveInstanceState()
            cartDataSource!!.updateCart(event.cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object:SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                        calculateTotalPrice()
                        recycler_cart!!.layoutManager!!.onRestoreInstanceState(recyclerViewState)
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context,"[ACTUALIZAR CANASTA]"+e.message,Toast.LENGTH_SHORT).show()
                    }

                })
        }
    }

    private fun calculateTotalPrice() {
        cartDataSource!!.sumPrice(Commun.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Double>{
                override fun onSuccess(price: Double) {
                    txt_total_price!!.text = StringBuilder("Total: $")
                        .append(Commun.formatPrice(price))
                }

                override fun onSubscribe(d: Disposable) {
                 }

                override fun onError(e: Throwable) {
                    if(!e.message!!.contains("Query returned empty"))
                            Toast.makeText(context,"[SUMAR CANASTA]"+e.message,Toast.LENGTH_SHORT).show()
                }

            })
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu!!.findItem(R.id.action_editar).setVisible(false)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater!!.inflate(R.menu.cart_menu,menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item!!.itemId == R.id.action_clear_cart)
        {
            cartDataSource!!.cleanCart(Commun.currentUser!!.uid!!)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                        Toast.makeText(context,"Canasta vaciada satisfactoriamente",Toast.LENGTH_SHORT).show()
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context,""+e.message,Toast.LENGTH_SHORT).show()
                    }

                })
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSearchFound(category: CategoryModel, cartItem: CartItem){
        val foodModel:FoodModel = Commun.findFoodInListById(category,cartItem!!.foodId)!!
        if(foodModel != null)
            showUpdateDialog(cartItem,foodModel)
        else
            Toast.makeText(context!!,"ID del plato no encontrado",Toast.LENGTH_SHORT).show()
    }

    private fun showUpdateDialog(cartItem: CartItem, foodModel: FoodModel) {
        Commun.foodSelected = foodModel
        val builder = AlertDialog.Builder(context!!)
        val itemView:View = LayoutInflater.from(context!!).inflate(R.layout.layout_dialog_update_cart,null)
        builder.setView(itemView)

        val btn_ok = itemView.findViewById<View>(R.id.btn_ok) as Button
        val btn_cancel = itemView.findViewById<View>(R.id.btn_cancel) as Button

        val rdi_group_size = itemView.findViewById<View>(R.id.rdi_group_size) as RadioGroup
        chip_group_user_seleted_addon = itemView.findViewById<View>(R.id.chip_group_user_selected_addon)as ChipGroup
        val img_addon_on = itemView.findViewById<View>(R.id.img_add_addon) as ImageView

        img_addon_on.setOnClickListener{
            if(foodModel.addon != null)
            {
                displayAddonList()
                addonBottomSheetDialog!!.show()
            }
        }

        if(foodModel.size != null)
        {
            for(sizeModel in foodModel.size){
                val radioButton = RadioButton(context)
                radioButton.setOnCheckedChangeListener{compoundButton, b ->
                    if(b) Commun.foodSelected!!.userSelectedSize = sizeModel
                    calculateTotalPrice()
                }
                val params = LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1.0f)
                radioButton.layoutParams = params
                radioButton.setText(sizeModel.name)
                radioButton.tag = (sizeModel.price)
                rdi_group_size.addView(radioButton)
            }
            if(rdi_group_size.childCount > 0)
            {
                val radioButton = rdi_group_size.getChildAt(0)as RadioButton
                radioButton.isChecked = true
            }
        }

        displayAlreadySelectedAddon(chip_group_user_seleted_addon!!,cartItem)

        val dialog = builder.create()
        dialog.show()

        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window!!.setGravity(Gravity.CENTER)

        btn_cancel.setOnClickListener{dialog.dismiss()}
        btn_ok.setOnClickListener {
            cartDataSource!!.deleteCart(cartItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object:SingleObserver<Int>{
                    override fun onSuccess(t: Int) {
                       if(Commun.foodSelected!!.userSelectedAddon != null)
                        cartItem.foodAddon = Gson().toJson(Commun.foodSelected!!.userSelectedAddon)
                        else
                           cartItem.foodAddon="Default"
                        if (Commun.foodSelected!!.userSelectedSize != null)
                            cartItem.foodSize = Gson().toJson(Commun.foodSelected!!.userSelectedSize)
                        else
                            cartItem.foodSize="Default"

                        cartItem.foodExtraPrice = Commun.calculateExtraPrice(Commun.foodSelected!!.userSelectedSize,
                        Commun.foodSelected!!.userSelectedAddon!!)

                        compositeDisposable.add(cartDataSource!!.insertOrReplaceAll(cartItem)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                EventBus.getDefault().postSticky(CountCartEvent(true))
                                dialog.dismiss()
                                calculateTotalPrice()
                                Toast.makeText(context,"Actualización exitosa de tu canasta",Toast.LENGTH_SHORT).show()

                            }, { t: Throwable? ->
                                Toast.makeText(
                                    context, "[INSERTAR CANASTA]" + t!!.message,
                                    Toast.LENGTH_SHORT
                                ).show()

                            }))
                    }

                    override fun onSubscribe(d: Disposable) {

                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(context!!,e.message,Toast.LENGTH_SHORT).show()
                    }

                })
        }
    }

    private fun displayAlreadySelectedAddon(
        chipGroupUserSeletedAddon: ChipGroup,
        cartItem: CartItem
    ) {
        if(cartItem.foodAddon != null && !cartItem.equals("Default"))
        {
            val addonModels:List<AddonModel> = Gson().fromJson(cartItem.foodAddon,object:TypeToken<List<AddonModel>>(){}.type)
            Commun.foodSelected!!.userSelectedAddon = addonModels.toMutableList()
            chipGroupUserSeletedAddon.removeAllViews()

            for(addonModel in addonModels)
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("($")
                    .append(addonModel.price).append(")")
                chip.isClickable=false
                chip.setOnCloseIconClickListener {
                    chipGroupUserSeletedAddon.removeView(it)
                    Commun.foodSelected!!.userSelectedAddon!!.remove(addonModel)
                    calculateTotalPrice()
                }
                chipGroupUserSeletedAddon.addView(chip)
            }
        }
    }

    private fun displayAddonList() {
        if(Commun.foodSelected!!.addon !=null && Commun.foodSelected!!.addon.size > 0)
        {
            chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()
            edt_search_addon!!.addTextChangedListener(this)

            for(addonModel in Commun.foodSelected!!.addon)
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip,null) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("($")
                    .append(addonModel.price).append(")")
                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if(b)
                    {
                        if(Commun.foodSelected!!.userSelectedAddon == null)
                            Commun.foodSelected!!.userSelectedAddon = ArrayList()
                        Commun.foodSelected!!.userSelectedAddon!!.add(addonModel)
                    }
                    chip_group_addon!!.addView(chip)
                }
            }
        }
    }

    override fun onSearchNoFound(message: String) {
        Toast.makeText(context!!,message,Toast.LENGTH_SHORT).show()
    }

    override fun afterTextChanged(p0: Editable?) {

    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()

        for(addonModel in Commun.foodSelected!!.addon)
        {
            if(addonModel.name!!.toLowerCase().contains(p0.toString().toLowerCase())) {
                val chip = layoutInflater.inflate(R.layout.layout_chip, null) as Chip
                chip.text = StringBuilder(addonModel.name!!).append("($")
                    .append(addonModel.price).append(")")
                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if (b) {
                        if (Commun.foodSelected!!.userSelectedAddon == null)
                            Commun.foodSelected!!.userSelectedAddon = ArrayList()
                        Commun.foodSelected!!.userSelectedAddon!!.add(addonModel)
                    }
                    chip_group_addon!!.addView(chip)
                }
            }
        }
    }
}
