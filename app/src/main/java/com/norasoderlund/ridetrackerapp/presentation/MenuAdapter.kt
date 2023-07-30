package com.norasoderlund.ridetrackerapp.presentation

import com.norasoderlund.ridetrackerapp.R
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView


data class MenuItem(val label: String, val resourceId: Int, val tintColor: Int, val onClickListener: OnClickListener);

class MyAdapter(private val itemList: List<MenuItem>) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(@NonNull holder: MyViewHolder, position: Int) {
        val itemText = itemList[position]
        holder.itemTextView.text = itemText.label;

        holder.buttonImage.setImageResource(itemText.resourceId);
        holder.buttonLayout.background.setTint(itemText.tintColor);

        // Handle button click here if needed.
        holder.itemButton.setOnClickListener(itemText.onClickListener)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    class MyViewHolder(@NonNull itemView: View) : RecyclerView.ViewHolder(itemView) {
        var itemTextView: TextView
        var itemButton: LinearLayout

        var buttonLayout: LinearLayout;
        var buttonImage: ImageView;

        init {
            itemTextView = itemView.findViewById<TextView>(R.id.buttonLabel)
            itemButton = itemView.findViewById(R.id.button)

            buttonLayout = itemView.findViewById(R.id.buttonLayout);
            buttonImage = itemView.findViewById(R.id.buttonImage);
        }
    }
}