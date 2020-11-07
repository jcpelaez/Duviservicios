package com.example.duviservicios.ui.bebidaslist

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.duviservicios.Adapter.MyBebidasListaAdapter
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.EventBus.MenuItemBack
import com.example.duviservicios.Model.BebidasModel
import com.example.duviservicios.R
import com.example.duviservicios.ui.bebidaslist.BebidasListViewModel
import org.greenrobot.eventbus.EventBus

class BebidasListFragment : Fragment() {

    private lateinit var bebidasListViewModel: BebidasListViewModel

    var recycler_food_list : RecyclerView?=null
    var layoutAnimationController:LayoutAnimationController?=null

    var adapter : MyBebidasListaAdapter?=null

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
        bebidasListViewModel =
            ViewModelProviders.of(this).get(BebidasListViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_bebidas_list, container, false)

        initViews(root)
        bebidasListViewModel.getmutableFoodModelListData().observe(this, Observer {
           if(it != null) {
               adapter = MyBebidasListaAdapter(context!!, it)
               recycler_food_list!!.adapter = adapter
               recycler_food_list!!.layoutAnimation = layoutAnimationController
           }
        })
        return root
    }

    private fun initViews(root: View?) {

        setHasOptionsMenu(true)

        recycler_food_list = root!!.findViewById(R.id.recycler_food_list) as RecyclerView
        recycler_food_list!!.setHasFixedSize(true)
        recycler_food_list!!.layoutManager = LinearLayoutManager(context)

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)

      }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search_menu,menu)

        val menuItem = menu.findItem(R.id.action_search)

        val searchManager = requireActivity().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menuItem.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))

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
            bebidasListViewModel.getmutableFoodModelListData()
        }
    }

    private fun startSearch(s: String) {
        val resultFood = ArrayList<BebidasModel>()
        /*for (i in 0 until Commun.categorySeleted!!.foods!!.size)
        {
            val foodModel = Commun.categorySeleted!!.foods!![i]
            if(foodModel.name!!.toLowerCase().contains(s))
                resultFood.add(foodModel)
        }*/
        bebidasListViewModel.getmutableFoodModelListData().value = resultFood
    }
}