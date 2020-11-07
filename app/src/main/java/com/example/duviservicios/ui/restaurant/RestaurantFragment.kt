package com.example.duviservicios.ui.restaurant

import android.app.AlertDialog
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duviservicios.Adapter.MyRestaurantAdapter
import com.example.duviservicios.R
import dmax.dialog.SpotsDialog


class RestaurantFragment : Fragment() {

    companion object {
        fun newInstance() =
            RestaurantFragment()
    }

    private lateinit var dialog: AlertDialog
    private lateinit var layoutAnimationController: LayoutAnimationController
    private var adapter: MyRestaurantAdapter?=null

    private var recycler_restaurant: RecyclerView?=null

    private lateinit var viewModel: RestaurantViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProviders.of(this).get(RestaurantViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_restaurant, container, false)
        initViews(root)

        viewModel.getMessajeError().observe(this, Observer {
            Toast.makeText(context,it,Toast.LENGTH_SHORT).show()
        })

        viewModel.getRestaurantList().observe(this, Observer {
            dialog.dismiss()
            adapter = MyRestaurantAdapter(context!!,it)
            recycler_restaurant!!.adapter = adapter
            recycler_restaurant!!.layoutAnimation = layoutAnimationController
        })
        return root
    }

    private fun initViews(root: View?) {
        setHasOptionsMenu(true)

        dialog = SpotsDialog.Builder().setContext(context)
            .setCancelable(false).build()
        dialog.show()
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        recycler_restaurant = root!!.findViewById(R.id.recycler_restaurant) as RecyclerView
        recycler_restaurant!!.setHasFixedSize(true)
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL

        recycler_restaurant!!.layoutManager = layoutManager
        recycler_restaurant!!.addItemDecoration(DividerItemDecoration(context,layoutManager.orientation))
    }

}