package com.example.duviservicios

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.Commun.MyCustomInfoWindow
import com.example.duviservicios.Model.ShippingOrderModel
import com.example.duviservicios.Remote.IGoogleAPI
import com.example.duviservicios.Remote.RetrofitGoogleAPIClient

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_tracking_order.*
import org.json.JSONObject
import java.lang.Exception
import java.lang.StringBuilder
import java.util.ArrayList

class TrackingOrderActivity : AppCompatActivity(), OnMapReadyCallback, ValueEventListener {

    private lateinit var mMap: GoogleMap

    private var shippperMarker: Marker?=null
    private var polylineOptions: PolylineOptions?=null
    private var blackPolylineOptions: PolylineOptions?=null
    private var blackPolyline:Polyline?=null
    private var grayPolyline:Polyline?=null
    private var redPolyline:Polyline?=null
    private var polylineList:List<LatLng> = ArrayList()

    private lateinit var iGoogleAPI: IGoogleAPI
    private val compositeDisposable = CompositeDisposable()

    private var isInit = false
    private lateinit var shipperRef:DatabaseReference
    private var handler:Handler?=null
    private var index =0
    private var next:Int=0
    private var v =0f
    private var lat = 0.0
    private var lng=0.0
    private var startPosition=LatLng(0.0,0.0)
    private var endPosition=LatLng(0.0,0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_order)

        iGoogleAPI = RetrofitGoogleAPIClient.instance!!.create(IGoogleAPI::class.java)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        subscribeShipperMove()

        initView()
    }

    private fun initView() {
        btn_call.setOnClickListener {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse(
                StringBuilder("tel:").append(Commun.currentShippingOrder!!.shipperPhone!!)
                    .toString()
            )

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                Dexter.withActivity(this)
                    .withPermission(Manifest.permission.CALL_PHONE)
                    .withListener(object : PermissionListener {
                        override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                        }

                        override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                            Toast.makeText(
                                this@TrackingOrderActivity,
                                "Tu debes habilitar este permiso para llamar",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            p0: PermissionRequest?,
                            p1: PermissionToken?
                        ) {

                        }

                    }).check()
                return@setOnClickListener
            }
            startActivity(intent)
        }
    }

    private fun subscribeShipperMove() {
        shipperRef = FirebaseDatabase.getInstance()
            .getReference(Commun.SHIPPING_ORDER_REF)
            .child(Commun.currentShippingOrder!!.key!!)
        shipperRef.addValueEventListener(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setInfoWindowAdapter(MyCustomInfoWindow(layoutInflater))

        mMap!!.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_light_with_label))
            if(!success)
                Log.d("Duviservicios","Fallo al cargar el estilo del mapa")

        } catch (ex: Resources.NotFoundException)
        {
            Log.d("Duviservicios","No se encontro el sting json para el mapa")
        }

        drawRouter()
    }

    private fun drawRouter() {
        val locationOrder = LatLng(Commun.currentShippingOrder!!.orderModel!!.lat,
        Commun.currentShippingOrder!!.orderModel!!.lng)
        val locationShipper = LatLng(Commun.currentShippingOrder!!.currentLat,Commun.currentShippingOrder!!.currentLng)

        mMap.addMarker(MarkerOptions()
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
            .title(Commun.currentShippingOrder!!.orderModel!!.userName)
            .snippet(Commun.currentShippingOrder!!.orderModel!!.shippingAddress)
            .position(locationOrder))

        if(shippperMarker == null)
        {
            val height = 80
            val width = 80
            val bitmapDrawable = ContextCompat.getDrawable(this@TrackingOrderActivity, R.drawable.shippernew)
            as BitmapDrawable
            val resize = Bitmap.createScaledBitmap(bitmapDrawable.bitmap,width,height,false)

            shippperMarker = mMap.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(resize))
                .title(StringBuilder("Repartidor: ").append(Commun.currentShippingOrder!!.shipperName).toString())
                .snippet(StringBuilder("Teléfono: ").append(Commun.currentShippingOrder!!.shipperPhone)
                    .append("\n")
                    .append("Tiempo estimado de entrega: ")
                    .append(Commun.currentShippingOrder!!.estimateTime).toString())
                .position(locationShipper))

            shippperMarker!!.showInfoWindow()

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18.0f))
        } else
        {
            shippperMarker!!.position = locationShipper
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper,18.0f))
        }

        val to = StringBuilder().append(Commun.currentShippingOrder!!.orderModel!!.lat)
            .append(",")
            .append(Commun.currentShippingOrder!!.orderModel!!.lng)
            .toString()

        val from = StringBuilder().append(Commun.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Commun.currentShippingOrder!!.currentLng)
            .toString()

        compositeDisposable.add(iGoogleAPI!!.getDirections("driving","less_driving",
            from,to,
            getString(R.string.google_maps_key))!!
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ s ->
                try {
                    val jsonObject = JSONObject(s)
                    val jsonArray = jsonObject.getJSONArray("routes")
                    for (i in 0 until jsonArray.length()) {
                        val route = jsonArray.getJSONObject(i)
                        val poly = route.getJSONObject("overview_polyline")
                        val polyline = poly.getString("points")
                        polylineList = Commun.decodePoly(polyline)
                    }

                    polylineOptions = PolylineOptions()
                    polylineOptions!!.color(Color.RED)
                    polylineOptions!!.width(12.0f)
                    polylineOptions!!.startCap(SquareCap())
                    polylineOptions!!.endCap(SquareCap())
                    polylineOptions!!.jointType(JointType.ROUND)
                    polylineOptions!!.addAll(polylineList)
                    redPolyline = mMap.addPolyline(polylineOptions)


                } catch (e: Exception) {
                    Log.d("DEBUG", e.message!!)
                }

            }, { Throwable ->
                Toast.makeText(
                    this@TrackingOrderActivity,
                    "" + Throwable.message,
                    Toast.LENGTH_SHORT
                ).show()
            }))
    }

    override fun onDestroy() {
        shipperRef.removeEventListener(this)
        isInit = false
        super.onDestroy()
    }
    override fun onDataChange(snapshot: DataSnapshot) {
        val from = StringBuilder()
            .append(Commun.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Commun.currentShippingOrder!!.currentLng)
            .toString()
        Commun.currentShippingOrder = snapshot.getValue(ShippingOrderModel::class.java)
        Commun.currentShippingOrder!!.key = snapshot.key

        val to = StringBuilder()
            .append(Commun.currentShippingOrder!!.currentLat)
            .append(",")
            .append(Commun.currentShippingOrder!!.currentLng)
            .toString()
        Commun.currentShippingOrder = snapshot.getValue(ShippingOrderModel::class.java)
        Commun.currentShippingOrder!!.key = snapshot.key

        if (snapshot.exists())
            if(isInit) moveMakerAnimation(shippperMarker,from,to) else isInit= true
    }

    private fun moveMakerAnimation(shippperMarker: Marker?, from: String, to: String) {

            val add = compositeDisposable.add(iGoogleAPI!!.getDirections(
                "driving", "less_driving",
                from,
                to,
                getString(R.string.google_maps_key)
            )!!
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ s ->
                    Log.d("DEBUG", s)
                    try {
                        val jsonObject = JSONObject(s)
                        val jsonArray = jsonObject.getJSONArray("routes")
                        for (i in 0 until jsonArray.length()) {
                            val route = jsonArray.getJSONObject(i)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Commun.decodePoly(polyline)
                        }

                        polylineOptions = PolylineOptions()
                        polylineOptions!!.color(Color.GRAY)
                        polylineOptions!!.width(5.0f)
                        polylineOptions!!.startCap(SquareCap())
                        polylineOptions!!.endCap(SquareCap())
                        polylineOptions!!.jointType(JointType.ROUND)
                        polylineOptions!!.addAll(polylineList)
                        grayPolyline = mMap.addPolyline(polylineOptions)

                        blackPolylineOptions = PolylineOptions()
                        blackPolylineOptions!!.color(Color.BLACK)
                        blackPolylineOptions!!.width(5.0f)
                        blackPolylineOptions!!.startCap(SquareCap())
                        blackPolylineOptions!!.endCap(SquareCap())
                        blackPolylineOptions!!.jointType(JointType.ROUND)
                        blackPolylineOptions!!.addAll(polylineList)
                        blackPolyline = mMap.addPolyline(blackPolylineOptions)

                        val polylineAnimator = ValueAnimator.ofInt(0, 100)
                        polylineAnimator.setDuration(2000)
                        polylineAnimator.setInterpolator(LinearInterpolator())
                        polylineAnimator.addUpdateListener { valueAnimator ->
                            val points = grayPolyline!!.points
                            val percentValue = Integer.parseInt(valueAnimator.animatedValue.toString())
                            val size = points.size
                            val newPoints = (size * (percentValue / 100.0f)).toInt()
                            val p = points.subList(0, newPoints)
                            blackPolyline!!.points = p
                        }
                        polylineAnimator.start()

                        handler = Handler()
                        index = -1
                        next = 1
                        handler!!.postDelayed(Runnable {

                        },1500)

                        val r = object : Runnable {
                            override fun run() {
                                if (index < polylineList.size - 1) {
                                    index++
                                    next = index + 1
                                    startPosition = polylineList[index]
                                    endPosition = polylineList[index]
                                }

                                val valueAnimator = ValueAnimator.ofInt(0, 1)
                                valueAnimator.setDuration(1500)
                                valueAnimator.setInterpolator(LinearInterpolator())
                                valueAnimator.addUpdateListener { valueAnimator ->
                                    v = valueAnimator.animatedFraction
                                    lat =
                                        v * endPosition!!.latitude + (1 - v) * startPosition!!.latitude
                                    lng =
                                        v * endPosition!!.longitude + (1 - v) * startPosition!!.longitude

                                    val newPos = LatLng(lat, lng)
                                    shippperMarker!!.position = newPos
                                    shippperMarker!!.setAnchor(0.5f, 0.5f)
                                    shippperMarker!!.rotation = Commun.getBearing(startPosition!!, newPos)

                                    mMap.moveCamera(CameraUpdateFactory.newLatLng(shippperMarker.position))
                                }

                                valueAnimator.start()
                                if (index < polylineList.size - 2)
                                    handler!!.postDelayed(this, 1500)
                            }

                        }

                        handler = Handler()
                        handler!!.postDelayed(r, 1500)

                    } catch (e: Exception) {
                        Log.d("DEBUG", e.message!!)
                    }

                }, { Throwable ->
                    Toast.makeText(
                        this@TrackingOrderActivity,
                        "" + Throwable.message,
                        Toast.LENGTH_SHORT
                    ).show()
                })
            )

    }

    override fun onCancelled(error: DatabaseError) {

    }
}