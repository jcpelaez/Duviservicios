package com.example.duviservicios.ui.foodlist

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.duviservicios.Adapter.MyFoodListaAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.EventBus.MenuItemBack
import com.example.duviservicios.Model.FoodModel
import com.example.duviservicios.R
import com.google.android.gms.common.internal.service.Common
import org.greenrobot.eventbus.EventBus

class FoodListFragment : Fragment() {

    private lateinit var foodListViewModel: FoodListViewModel

    var recycler_food_list : RecyclerView?=null
    var layoutAnimationController:LayoutAnimationController?=null

    var adapter : MyFoodListaAdapter?=null

    override fun onStop() {
        if (adapter != null)
            super.onStop()
    }

    override fun onDestroy() {
        EventBus.getDefault().postSticky(MenuItemBack())
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        foodListViewModel =
            ViewModelProviders.of(this).get(FoodListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_food_list, container, false)

        initViews(root)
        foodListViewModel.getmutableFoodModelListData().observe(this, Observer {
            if(it != null) {
                adapter = MyFoodListaAdapter(context!!, it)
                recycler_food_list!!.adapter = adapter
                recycler_food_list!!.layoutAnimation = layoutAnimationController
            }
        })
        return root
    }

    private fun initViews(root: View?) {

        setHasOptionsMenu(true)

        (activity as AppCompatActivity).supportActionBar!!.setTitle(Commun.categorySeleted!!.name)

        recycler_food_list = root!!.findViewById(R.id.recycler_food_list) as RecyclerView
        recycler_food_list!!.setHasFixedSize(true)
        recycler_food_list!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

        (activity as AppCompatActivity).supportActionBar!!.title = Commun.categorySeleted!!.name
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu,menu)

        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = activity!!.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(activity!!.componentName))

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String?): Boolean {
                startSearch(s!!)
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })

        val closeButton = searchView.findViewById<View>(R.id.search_close_btn) as ImageView
        closeButton.setOnClickListener{
            val ed = searchView.findViewById<View>(R.id.search_src_text)as EditText
            ed.setText("")
            searchView.setQuery("",false)
            searchView.onActionViewCollapsed()
            menuItem.collapseActionView()
            foodListViewModel.getmutableFoodModelListData()
        }
    }

    private fun startSearch(s: String) {
        val resultFood = ArrayList<FoodModel>()
        for (i in 0 until Commun.categorySeleted!!.foods!!.size)
        {
            val foodModel = Commun.categorySeleted!!.foods!![i]
            if(foodModel.name!!.toLowerCase().contains(s))
                resultFood.add(foodModel)
        }
        foodListViewModel.getmutableFoodModelListData().value = resultFood
    }
}