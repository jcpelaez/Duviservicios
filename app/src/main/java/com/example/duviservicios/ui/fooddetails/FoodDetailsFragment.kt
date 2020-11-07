package com.example.duviservicios.ui.fooddetails

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.andremion.counterfab.CounterFab
import com.bumptech.glide.Glide
import com.cepheuen.elegantnumberbutton.view.ElegantNumberButton
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Database.CartDataSource
import com.example.duviservicios.Database.CartDatabase
import com.example.duviservicios.Database.CartItem
import com.example.duviservicios.Database.LocalCartDataSource
import com.example.duviservicios.EventBus.CountCartEvent
import com.example.duviservicios.EventBus.MenuItemBack
import com.example.duviservicios.EventBus.UpdateItemInCart
import com.example.duviservicios.Model.CommentModel
import com.example.duviservicios.Model.FoodModel
import com.example.duviservicios.R
import com.example.duviservicios.ui.comment.CommentFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import com.google.gson.Gson
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.fuseable.HasUpstreamPublisher
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_rating_comment.*
import org.greenrobot.eventbus.EventBus
import java.lang.StringBuilder

class FoodDetailsFragment : Fragment(), TextWatcher {
    override fun afterTextChanged(p0: Editable?) {

    }

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

    }

    override fun onTextChanged(charSequence: CharSequence?, p1: Int, p2: Int, p3: Int) {
        chip_group_addon!!.clearCheck()
        chip_group_addon!!.removeAllViews()
        for(addonModel in Commun.foodSelected!!.addon!!)
        {
            if(addonModel.name!!.toLowerCase().contains(charSequence.toString().toLowerCase()))
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip,null,false) as Chip
                chip.text = StringBuilder(addonModel!!.name!!).append("(+$").append(addonModel.price).append(")").toString()
                chip.setOnCheckedChangeListener { compoundButton, b ->
                    if (b) {
                        if(Commun.foodSelected!!.userSelectedAddon == null)
                            Commun.foodSelected!!.userSelectedAddon = ArrayList()
                        Commun.foodSelected!!.userSelectedAddon!!.add(addonModel)
                    }
                }
                chip_group_addon!!.addView(chip)
            }
        }

    }

    private lateinit var foodDetailsViewModel: FoodDetailsViewModel
    private lateinit var addonBottomSheetDialog: BottomSheetDialog

    private val compositeDisposable = CompositeDisposable()
    private lateinit var cartDataSource : CartDataSource

    private var img_food:ImageView?=null
    private var btnCart:CounterFab?=null
    private var btnRating:FloatingActionButton?=null
    private var food_name:TextView?=null
    private var food_descripcion:TextView?=null
    private var food_price:TextView?=null
    private var number_button:ElegantNumberButton?=null
    private var ratingBar:RatingBar?=null
    private var btnShowComment:Button?=null
    private var rdi_group_size:RadioGroup?=null
    private var img_add_on:ImageView?=null
    private var chip_group_user_selected_addon:ChipGroup?=null

    private var chip_group_addon:ChipGroup?=null
    private var edt_search_addon : EditText?=null

    private var waitingDialog:AlertDialog?=null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodDetailsViewModel =
            ViewModelProviders.of(this).get(FoodDetailsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_details, container, false)

        initViews(root)

        foodDetailsViewModel.getmutableLiveDataFood().observe(this, Observer {
            displayInfo(it)
        })

        foodDetailsViewModel.getMutableLiveDataComment().observe(this, Observer {
            submitRatingToFirebase(it)
        })
        return root
    }

    private fun submitRatingToFirebase(commentModel: CommentModel?) {
        waitingDialog!!.show()

        FirebaseDatabase.getInstance()
            .getReference(Commun.COMMENT_REF)
            .child(Commun.foodSelected!!.id!!)
            .push()
            .setValue(commentModel)
            .addOnCompleteListener{task ->
                if(task.isSuccessful)
                {
                    addRatingToFood(commentModel!!.ratingValue.toDouble())
                }
                waitingDialog!!.dismiss()
            }
    }

    private fun addRatingToFood(ratingValue: Double) {
        FirebaseDatabase.getInstance()
            .getReference(Commun.CATEGORY_REF)// seleccionamos la categoria
            .child(Commun.categorySeleted!!.menu_id!!)// seleccionamos el id del menu en la categoria)
            .child("foods")// seleccionamos el id del plato
            .child(Commun.foodSelected!!.key!!)// seleccionamos la llave
            .addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    waitingDialog!!.dismiss()
                    Toast.makeText(context!!,""+p0.message,Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.exists())
                    {
                        val foodModel = dataSnapshot.getValue(FoodModel::class.java)
                        foodModel!!.key = Commun.foodSelected!!.key

                        //aplico el rankin
                        val sumRating = foodModel.ratingValue.toDouble() + (ratingValue)
                        val ratingCount = foodModel.ratingCount + 1

                        val updateData = HashMap<String,Any>()
                        updateData["ratingValue"]= sumRating
                        updateData["ratingCount"] = ratingCount

                        //actualizamos los datos
                        foodModel.ratingCount = ratingCount
                        foodModel.ratingValue = sumRating

                        dataSnapshot.ref
                            .updateChildren(updateData)
                            .addOnCompleteListener{task ->
                                waitingDialog!!.dismiss()
                                if(task.isSuccessful)
                                {
                                    Commun.foodSelected = foodModel
                                    foodDetailsViewModel!!.setFoodModel(foodModel)
                                    Toast.makeText(context!!,"Gracias por compartir tus comentarios",Toast.LENGTH_SHORT).show()
                                }
                            }

                    } else
                        waitingDialog!!.dismiss()
                }

            })

    }

    private fun displayInfo(it: FoodModel?) {
        Glide.with(context!!).load(it!!.image).into(img_food!!)
        food_name!!.text = StringBuilder(it!!.name!!)
        food_descripcion!!.text = StringBuilder(it!!.description!!)
        food_price!!.text = StringBuilder(it!!.price.toInt() /2)

        ratingBar!!.rating = it!!.ratingValue.toFloat() / it!!.ratingCount

        //Muestra el tamaño del plato
        for (sizesModel in it!!.size)
        {
           val radioButton = RadioButton(context)
            radioButton.setOnCheckedChangeListener{ compoundButton, b ->
                if(b)
                {
                    Commun.foodSelected!!.userSelectedSize = sizesModel
                    calculateTotalPrice()
                }

                val params = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT,1.0f)

               // val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                 //  LinearLayout.LayoutParams.WRAP_CONTENT,1.0f)
                radioButton.layoutParams = params
                radioButton.text = sizesModel.name
                radioButton.tag = sizesModel.price

                rdi_group_size!!.addView(radioButton)
            }

            //defecto cuando arranca la aplicacion
            if(rdi_group_size!!.childCount > 0)
                {
                    val radioButton = rdi_group_size!!.getChildAt(0) as RadioButton
                    radioButton.isChecked = true
               }
        }
        calculateTotalPrice()
    }

    private fun calculateTotalPrice() {
        var totalPrice = Commun.foodSelected!!.price.toDouble()
        var displayPrice = 0.0

        //addon
        if(Commun.foodSelected!!.userSelectedAddon != null && Commun.foodSelected!!.userSelectedAddon!!.size > 0)
        {
            for(addonModel in Commun.foodSelected!!.userSelectedAddon!!)
                totalPrice += addonModel.price!!.toDouble()
        }

        totalPrice += Commun.foodSelected!!.price!!.toDouble()
        //tamaño
//        totalPrice += Commun.foodSelected!!.userSelectedSize!!.price!!.toDouble()

        displayPrice = totalPrice * number_button!!.number.toInt()
        displayPrice = Math.round(displayPrice * 100.0)/100.0

        food_price!!.text = StringBuilder("").append(Commun.formatPrice(displayPrice)).toString()
    }

    private fun initViews(root: View?) {

        (activity as AppCompatActivity).supportActionBar!!.setTitle(Commun.foodSelected!!.name)

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(context!!).cartDAO())

        addonBottomSheetDialog = BottomSheetDialog(context!!,R.style.DialogStyle)
        val layout_user_selected_addon = layoutInflater.inflate(R.layout.layout_addon_display,null)
        chip_group_addon = layout_user_selected_addon.findViewById(R.id.chip_group_addon) as ChipGroup
        edt_search_addon = layout_user_selected_addon.findViewById(R.id.edt_search) as EditText
        addonBottomSheetDialog.setContentView(layout_user_selected_addon)

        addonBottomSheetDialog.setOnDismissListener{ dialogInterface ->
            displayUserSelectedAddon()
            calculateTotalPrice()
        }

        waitingDialog = SpotsDialog.Builder().setContext(context!!).setCancelable(false).build()

        btnCart = root!!.findViewById(R.id.btnCart) as CounterFab
        img_food = root!!.findViewById(R.id.img_food) as ImageView
        btnRating = root!!.findViewById(R.id.btn_rating) as FloatingActionButton
        food_name = root!!.findViewById(R.id.food_name) as TextView
        food_descripcion = root!!.findViewById(R.id.food_descripcion) as TextView
        food_price = root!!.findViewById(R.id.food_price) as TextView
        number_button = root!!.findViewById(R.id.number_button) as ElegantNumberButton
        ratingBar = root!!.findViewById(R.id.ratingBar) as RatingBar
        btnShowComment = root!!.findViewById(R.id.btnShowComment) as Button
        rdi_group_size = root!!.findViewById(R.id.rdi_group_size) as RadioGroup
        img_add_on = root!!.findViewById(R.id.img_add_addon) as ImageView
        chip_group_user_selected_addon = root!!.findViewById(R.id.chip_group_user_selected_addon) as ChipGroup


        img_add_on!!.setOnClickListener{
            if(Commun.foodSelected!!.addon != null)
            {
                displayAllAddon()
                addonBottomSheetDialog.show()
            }
        }

        number_button!!.setOnValueChangeListener { view, oldValue, newValue ->
            calculateTotalPrice()
        }


        btnRating!!.setOnClickListener {
            showDialogRating()
        }

        btnShowComment!!.setOnClickListener{
            val commentFragment = CommentFragment.getInstance()
            commentFragment.show(activity!!.supportFragmentManager,"CommentFragment")
        }

        btnCart!!.setOnClickListener{

            var cartItem = CartItem()
            cartItem.uid = Commun.currentUser!!.uid
            cartItem.userPhone = Commun.currentUser!!.phone

            cartItem.foodId = Commun.foodSelected!!.id!!
            cartItem.foodName = Commun.foodSelected!!.name!!
            cartItem.foodImage = Commun.foodSelected!!.image!!
            cartItem.foodPrice = Commun.foodSelected!!.price!!.toDouble()
            cartItem.foodQuantity = number_button!!.number.toInt()
            cartItem.foodExtraPrice = Commun.calculateExtraPrice(Commun.foodSelected!!.userSelectedSize,Commun.foodSelected!!.userSelectedAddon)

            if(Commun.foodSelected!!.userSelectedAddon != null)
                 cartItem.foodAddon = Gson().toJson(Commun.foodSelected!!.userSelectedAddon)
            else
                cartItem.foodSize = "Default"

            if(Commun.foodSelected!!.userSelectedSize != null)
                cartItem.foodSize = Gson().toJson(Commun.foodSelected!!.userSelectedSize)
            else
                cartItem.foodSize = "Default"


            cartDataSource.getItemWithAllOptionsInCart(

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
                                    Toast.makeText(context,"Producto agregado a tu canasta",Toast.LENGTH_SHORT).show()
                                    EventBus.getDefault().postSticky(CountCartEvent(true))
                                }, { t: Throwable? ->
                                    Toast.makeText(context,"[INSERTAR CANASTA]" + t!!.message, Toast.LENGTH_SHORT).show()
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
                                        "[INSERTAR CANASTA]" + t!!.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                })
                            )
                        } else
                            Toast.makeText(context,"[CANASTA ERROR]"+e.message,Toast.LENGTH_SHORT).show()
                    }
                })

        }

    }

    private fun displayAllAddon() {
        if(Commun.foodSelected!!.addon!!.size > 0)
        {
            chip_group_addon!!.clearCheck()
            chip_group_addon!!.removeAllViews()

            edt_search_addon!!.addTextChangedListener(this)

            for(addonModel in Commun.foodSelected!!.addon!!)
            {
                    val chip = layoutInflater.inflate(R.layout.layout_chip,null,false) as Chip
                    chip.text = StringBuilder(addonModel!!.name!!).append("($+").append(addonModel.price).append(")").toString()
                    chip.setOnCheckedChangeListener { compoundButton, b ->
                        if (b) {
                            if(Commun.foodSelected!!.userSelectedAddon == null)
                                Commun.foodSelected!!.userSelectedAddon = ArrayList()
                            Commun.foodSelected!!.userSelectedAddon!!.add(addonModel)
                        }
                    }
                    chip_group_addon!!.addView(chip)
            }
        }
    }

    private fun displayUserSelectedAddon() {
        if(Commun.foodSelected!!.userSelectedAddon != null && Commun.foodSelected!!.userSelectedAddon!!.size > 0)
        {
            chip_group_user_selected_addon!!.removeAllViews()
            for(addModel in Commun.foodSelected!!.userSelectedAddon!!)
            {
                val chip = layoutInflater.inflate(R.layout.layout_chip_with_delete,null,false) as Chip
                chip.text = StringBuilder(addModel!!.name!!).append("($+").append(addModel.price).append(")").toString()
                chip.isClickable = false
                chip.setOnClickListener { view ->
                    chip_group_user_selected_addon!!.removeView(view)
                    Commun.foodSelected!!.userSelectedAddon!!.remove(addModel)
                    calculateTotalPrice()
                }
                chip_group_user_selected_addon!!.addView(chip)
            }
        }
        else
           chip_group_user_selected_addon!!.removeAllViews()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    private fun showDialogRating() {
       var builder = AlertDialog.Builder(context!!)
        builder.setTitle("Calificar plato")
        builder.setMessage("Por favor ingrese la información")

        val itemView = LayoutInflater.from(context).inflate(R.layout.layout_rating_comment,null)

        val ratingBar = itemView.findViewById<RatingBar>(R.id.rating_bar)
        val edt_comment = itemView.findViewById<EditText>(R.id.edt_comment)

        builder.setView(itemView)

        builder.setNegativeButton("Cancelar"){dialogInterface, i -> dialogInterface.dismiss() }

        builder.setPositiveButton("OK"){dialogInterface, i ->
            val commentModel = CommentModel()
            //TODO
            commentModel.name = Commun.currentUser!!.name
           // commentModel.name = "Eddy Lee"
            commentModel.uid = Commun.currentUser!!.uid
            //commentModel.uid = "396695517652654"
            commentModel.comment = edt_comment.text.toString()
            commentModel.ratingValue = ratingBar.rating

            val serverTimeStamp = HashMap<String,Any>()
            serverTimeStamp["timeStamp"]= ServerValue.TIMESTAMP
            commentModel.commentTimeStamp = (serverTimeStamp)

            foodDetailsViewModel!!.setCommentModel(commentModel)
        }

        val dialog = builder.create()
        dialog.show()
    }
}