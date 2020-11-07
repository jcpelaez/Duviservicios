package com.example.duviservicios.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.duviservicios.Callback.IRecyclerItemClickListener
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.EventBus.PopularFoodItemClick
import com.example.duviservicios.Model.PopularCategoryModel
import com.example.duviservicios.R
import de.hdodenhof.circleimageview.CircleImageView
import org.greenrobot.eventbus.EventBus

class MyPopularCategoriasAdapter ( internal var context:Context,
                                    internal var popularCategoryModels: List<PopularCategoryModel>):
        RecyclerView.Adapter<MyPopularCategoriasAdapter.MyViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_popular_categorias_item,parent,false))
    }

    override fun getItemCount(): Int {
        return popularCategoryModels.size
    }


    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
       //Glide.with(context).load(popularCategoryModels.get(position).image).into(holder.category_image!!)
        //holder.category_name!!.setText(popularCategoryModels.get(position).name)

        holder.setListener (object:IRecyclerItemClickListener{
            override fun onItemClick(view: View, pos: Int) {
                EventBus.getDefault()
                    .postSticky(PopularFoodItemClick(popularCategoryModels[pos]))
            }

        })
    }

    inner class MyViewHolder(itemView : View) : RecyclerView.ViewHolder (itemView),
        View.OnClickListener {
        override fun onClick(p0: View?) {
            listener!!.onItemClick(p0!!, adapterPosition)
        }

        internal var listener: IRecyclerItemClickListener?=null

         fun setListener(listener: IRecyclerItemClickListener)
        {
            this.listener = listener
        }

        var category_name:TextView?=null
        var category_image:CircleImageView?=null

        init {
            //category_name = itemView.findViewById(R.id.txt_categoria_name) as TextView
            //category_image = itemView.findViewById(R.id.categoria_imagen) as CircleImageView


            itemView.setOnClickListener(this)
        }
    }

}