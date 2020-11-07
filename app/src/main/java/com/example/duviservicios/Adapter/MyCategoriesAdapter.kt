package com.example.duviservicios.Adapter

import android.content.Context
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.duviservicios.Callback.IRecyclerItemClickListener
import com.example.duviservicios.Commun.Commun
import com.example.duviservicios.EventBus.CategoryClick
import com.example.duviservicios.Model.CategoryModel
import com.example.duviservicios.Model.PopularCategoryModel
import com.example.duviservicios.R
import de.hdodenhof.circleimageview.CircleImageView
import org.greenrobot.eventbus.EventBus

class MyCategoriesAdapter (internal var context: Context,
                           internal var categoriesList: List<CategoryModel>):
    RecyclerView.Adapter<MyCategoriesAdapter.MyViewHolder>(){

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        Glide.with(context).load(categoriesList.get(position).image).into(holder.category_image!!)
        holder.category_name!!.setText(categoriesList.get(position).name)

        holder.setListener(object:IRecyclerItemClickListener{
            override fun onItemClick(view: View, pos: Int) {
               Commun.categorySeleted = categoriesList.get(pos)
                EventBus.getDefault().postSticky(CategoryClick(true,categoriesList.get(pos)))
            }

        })
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyCategoriesAdapter.MyViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_category_item,parent,false))
    }

    override fun getItemCount(): Int {
        return categoriesList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if(categoriesList.size == 1)
        Commun.DEFAULT_COLUMN_COUNT
        else
        {
            if (categoriesList.size % 2 == 0)
                Commun.DEFAULT_COLUMN_COUNT
            else
            {
                if (position > 1 && position == categoriesList.size-1) Commun.FULL_WIDTH_COLUMN else Commun.DEFAULT_COLUMN_COUNT
            }
        }

    }

    fun getCategoriesList(): List<CategoryModel> {
        return categoriesList
    }

    inner class MyViewHolder(itemView : View) : RecyclerView.ViewHolder (itemView),
        View.OnClickListener {
        override fun onClick(view: View?) {
            listener!!.onItemClick(view!!,adapterPosition)
        }

        var category_name: TextView?=null
        var category_image: ImageView?=null

        internal var listener:IRecyclerItemClickListener?=null

        fun setListener(listener: IRecyclerItemClickListener)
        {
            this.listener = listener
        }

        init {
           category_name = itemView.findViewById(R.id.txt_categoria_name) as TextView
            category_image = itemView.findViewById(R.id.categoria_imagen) as ImageView
            itemView.setOnClickListener(this)
        }
    }
    }