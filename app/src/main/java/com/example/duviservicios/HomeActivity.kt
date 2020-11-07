package com.example.duviservicios

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Database.CartDataSource
import com.example.duviservicios.Database.CartDatabase
import com.example.duviservicios.Database.LocalCartDataSource
import com.example.duviservicios.EventBus.*
import com.example.duviservicios.Model.CategoryModel
import com.example.duviservicios.Model.FoodModel
import com.example.duviservicios.Model.TransporteUserModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dmax.dialog.SpotsDialog
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.app_bar_home.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var cartDataSource: CartDataSource
    private lateinit var navController : NavController
    private var dialog: AlertDialog?=null
    private var drawer:DrawerLayout?=null
    private var navView:NavigationView?=null
    private lateinit var userRef: DatabaseReference


    override fun onResume() {
        super.onResume()
    }

    private var menuItemClick = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        cartDataSource = LocalCartDataSource(CartDatabase.getInstance(this).cartDAO())

        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { _ ->
            navController.navigate(R.id.nav_cart)
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.



        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_menu, R.id.nav_food_detail, R.id.nav_restaurant,
                R.id.nav_farmacia, R.id.nav_favor, R.id.nav_transporte, R.id.nav_mercados, R.id.nav_bebidas,
                R.id.nav_cart, R.id.nav_shared, R.id.nav_view_order, R.id.nav_loguot
            ), drawer
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView!!.setupWithNavController(navController)

        var headerView = navView!!.getHeaderView(0)
        var txt_user = headerView.findViewById<TextView>(R.id.txt_name_user)
        Commun.setSpanString("Hola, ", Commun.currentUser!!.name, txt_user)

        navView!!.setNavigationItemSelectedListener(object :
            NavigationView.OnNavigationItemSelectedListener {
            override fun onNavigationItemSelected(p0: MenuItem): Boolean {

                p0.isChecked = true
                drawer!!.closeDrawers()

                if (p0.itemId == R.id.nav_loguot) {
                    signOut()
                } else if (p0.itemId == R.id.nav_restaurant) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_restaurant)
                } else if (p0.itemId == R.id.nav_bebidas) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_bebidas)
                } else if (p0.itemId == R.id.nav_mercados) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_mercados)
                } else if (p0.itemId == R.id.nav_transporte) {
                    if (menuItemClick != p0.itemId)
                        showTransporteDialog()
                } else if (p0.itemId == R.id.nav_favor) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_farmacia)
                } else if (p0.itemId == R.id.nav_farmacia) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_farmacia)
                } else if (p0.itemId == R.id.nav_home) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_home)
                } else if (p0.itemId == R.id.nav_cart) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_cart)
                } else if (p0.itemId == R.id.nav_menu) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_menu)
                } else if (p0.itemId == R.id.nav_view_order) {
                    if (menuItemClick != p0.itemId)
                        navController.navigate(R.id.nav_view_order)
                } else if (p0.itemId == R.id.nav_update_info) {
                    showUpdateInfoDialog()
                } else if (p0.itemId == R.id.nav_shared) {
                    showCompartirAppDialog()
                } else if (p0.itemId == R.id.nav_llamar) {
                    llamar()
                }

                menuItemClick = p0.itemId
                return true
            }

        })

        countCartItem()

        userRef = FirebaseDatabase.getInstance().getReference(Commun.TRANSPORTE_REFERENCE)
    }

    private fun llamar() {
        Dexter.withActivity(this)
            .withPermission(android.Manifest.permission.CALL_PHONE)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    val Telefono = "3123327376"
                    val intent = Intent()
                    intent.setAction(Intent.ACTION_DIAL)
                    intent.setData(
                        Uri.parse(
                            StringBuilder("tel: ")
                                .append(Telefono).toString()
                        )
                    )
                    startActivity(intent)
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(
                        this@HomeActivity, "Tu debes aceptar este permiso " + p0!!.permissionName,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }).check()
    }



    private fun showTransporteDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Recogerme")
        builder.setMessage("Por favor ingrese toda la información para ir por usted")

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_transporte, null)
        val edt_origen = itemView.findViewById<View>(R.id.edt_address) as EditText
        val edt_destino = itemView.findViewById<View>(R.id.edt_llevo) as EditText
        val valor = "2000"

        builder.setNegativeButton("CANCELAR", { dialogInterface, _ -> dialogInterface.dismiss() })
            .setPositiveButton("OK", { _, _ ->
                if (TextUtils.isEmpty(edt_origen.text)) {
                    Toast.makeText(
                        this,
                        "Por favor diganos a donde te recogemos",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (TextUtils.isEmpty(edt_destino.text)) {
                    Toast.makeText(this, "Por favor diganos a donde te dejamos", Toast.LENGTH_SHORT)
                        .show()
                    return@setPositiveButton
                }

                val serverUserModel = TransporteUserModel()
                serverUserModel.uid = Commun.currentUser!!.uid.toString()
                serverUserModel.phone = Commun.currentUser!!.phone.toString()
                serverUserModel.origen = edt_origen.text.toString()
                serverUserModel.destino = edt_destino.text.toString()
                serverUserModel.isActive = true
                serverUserModel.valor = valor

                dialog!!.show()
                userRef!!.child(serverUserModel.uid!!)
                    .setValue(serverUserModel)
                    .addOnFailureListener{e ->
                        dialog!!.dismiss()
                        Toast.makeText(this,""+e.message,Toast.LENGTH_SHORT).show()
                    }
                    .addOnCompleteListener { _ ->
                        dialog!!.dismiss()
                        Toast.makeText(this,"Muy bien, ya te asignó un vehiculo y vamos por usted",Toast.LENGTH_SHORT).show()
                    }
            })

        builder.setView(itemView)

        val registerDialog = builder.create()
        registerDialog.show()
    }



    private fun showCompartirAppDialog() {
        val intent = Intent()
        intent.type = "text/plain"

        intent.putExtra(Intent.EXTRA_TEXT, "aqui va el texto de compartir")
        intent.putExtra(Intent.EXTRA_SUBJECT, "La estoy compartiendo desde la App Duviservicios")
        intent.action = Intent.ACTION_SEND
        val chooseIntent = Intent.createChooser(intent, "Compartir Aplicación Duviservicios")
        startActivity(chooseIntent)

    }

    private fun showUpdateInfoDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Registrar")
        builder.setMessage("Por favor ingrese toda la información")

        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)
        val edt_name = itemView.findViewById<View>(R.id.edt_name) as TextView
        val edt_phone = itemView.findViewById<View>(R.id.edt_phone) as TextView

        edt_phone.setText(Commun.currentUser!!.phone)
        edt_name.setText(Commun.currentUser!!.name)

        builder.setNegativeButton("CANCELAR", { dialogInterface, _ -> dialogInterface.dismiss() })
            .setPositiveButton("ACTUALIZAR", { _, _ ->
                if (TextUtils.isEmpty(edt_name.text)) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Por favor ingrese su nombre",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val update_data = HashMap<String, Any>()
                update_data.put("name", edt_name.text.toString())

                FirebaseDatabase.getInstance()
                    .getReference(Commun.USER_REFERENCE)
                    .child(Commun.currentUser!!.uid!!)
                    .updateChildren(update_data)
                    .addOnFailureListener {
                        Toast.makeText(this@HomeActivity, it.message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener {
                        Commun.currentUser!!.name = update_data["name"].toString()
                        Toast.makeText(
                            this@HomeActivity,
                            "Actualización exitosa",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            })

        builder.setView(itemView)

        val registerDialog = builder.create()
        registerDialog.show()
    }

    private fun signOut() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Salir")
            .setMessage("Realmente quieres salir de la aplicación?")
            .setNegativeButton("CANCELAR", { dialogInterface, _ -> dialogInterface.dismiss() })
            .setPositiveButton("OK"){ _, _ ->
                Commun.foodSelected = null
                Commun.categorySeleted = null
                Commun.currentUser = null
                FirebaseAuth.getInstance().signOut()

                val intent = Intent(this@HomeActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)

        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)

    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onMenuItemBack(event: MenuItemBack)
    {
        menuItemClick = -1
        if(supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack()
        event.toString()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onRestaurantClick(event: MenuItemEvent)
    {
        val bundle = Bundle()
        bundle.putString("restaurant", event.restaurantModel.uid)
        navController.navigate(R.id.nav_home, bundle)
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCategorySelect(event: CategoryClick)
    {
        if(event.isSuccess)
        {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_list)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onFoodSelected(event: FootItemClick)
    {
        if(event.isSuccess)
        {
            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_food_detail)
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onHideFABEvent(event: HideFABCart)
    {
        if(event.isHide)
        {
            fab.hide()
        }
        else
            fab.show()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onCountCartEvent(event: CountCartEvent)
    {
        if(event.isSuccess)
        {
            countCartItem()
        }

        countCartItem()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onPopularFoodItemClick(event: PopularFoodItemClick)
    {
        if(event.popularCategoryModel != null)
        {
            dialog!!.show()

            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.popularCategoryModel!!.menu_id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity, "" + p0.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()) {
                            Commun.categorySeleted = p0.getValue(CategoryModel::class.java)
                            Commun.categorySeleted!!.menu_id = p0.key

                            //cargamos el plato

                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.popularCategoryModel!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.popularCategoryModel.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(
                                            this@HomeActivity,
                                            "" + p0.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if (p0.exists()) {
                                            for (foodSnapShop in p0.children) {
                                                Commun.foodSelected = foodSnapShop.getValue(
                                                    FoodModel::class.java
                                                )
                                                Commun.foodSelected!!.key = foodSnapShop.key
                                            }
                                            navController!!.navigate(R.id.nav_food_detail)
                                        } else {
                                            Toast.makeText(
                                                this@HomeActivity,
                                                "Producto no existe",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        dialog!!.dismiss()
                                    }
                                })

                        } else {
                            dialog!!.dismiss()
                            Toast.makeText(
                                this@HomeActivity,
                                "Producto no existe",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
        }
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onBestDealFoodItemClick(event: BestDealItemClick)
    {
        if(event.model != null)
        {
            dialog!!.show()

            FirebaseDatabase.getInstance()
                .getReference("Category")
                .child(event.model!!.menu_id!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        dialog!!.dismiss()
                        Toast.makeText(this@HomeActivity, "" + p0.message, Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        if (p0.exists()) {
                            Commun.categorySeleted = p0.getValue(CategoryModel::class.java)
                            Commun.categorySeleted!!.menu_id = p0.key

                            //cargamos el plato

                            FirebaseDatabase.getInstance()
                                .getReference("Category")
                                .child(event.model!!.menu_id!!)
                                .child("foods")
                                .orderByChild("id")
                                .equalTo(event.model.food_id)
                                .limitToLast(1)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onCancelled(p0: DatabaseError) {
                                        dialog!!.dismiss()
                                        Toast.makeText(
                                            this@HomeActivity,
                                            "" + p0.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    override fun onDataChange(p0: DataSnapshot) {
                                        if (p0.exists()) {
                                            for (foodSnapShop in p0.children) {
                                                Commun.foodSelected = foodSnapShop.getValue(
                                                    FoodModel::class.java
                                                )
                                                Commun.foodSelected!!.key = foodSnapShop.key
                                            }
                                            navController!!.navigate(R.id.nav_food_detail)
                                        } else {
                                            Toast.makeText(
                                                this@HomeActivity,
                                                "Producto no existe",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        dialog!!.dismiss()
                                    }
                                })

                        } else {
                            dialog!!.dismiss()
                            Toast.makeText(
                                this@HomeActivity,
                                "Producto no existe",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                })
        }
    }


    private fun countCartItem() {

        cartDataSource.countItemInCart(Commun.currentUser!!.uid!!)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : SingleObserver<Int> {
                override fun onSuccess(t: Int) {
                    fab.count = t
                }

                override fun onSubscribe(d: Disposable) {

                }

                override fun onError(e: Throwable) {
                    if (!e.message!!.contains("Query returned empty"))
                        Toast.makeText(
                            this@HomeActivity,
                            "[CONTAR CANASTA]" + e.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    else
                        fab.count = 0
                }

            })
    }
}
