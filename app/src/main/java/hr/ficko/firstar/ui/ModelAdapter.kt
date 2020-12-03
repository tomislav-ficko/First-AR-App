package hr.ficko.firstar.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hr.ficko.firstar.R
import hr.ficko.firstar.models.Model
import kotlinx.android.synthetic.main.item_model.view.*

class ModelAdapter(val dataset: List<Model>) :
    RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

    class ModelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvTitle: TextView = view.tvTitle
        var ivThumbnail: ImageView = view.ivThumbnail
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        return ModelViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_model, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        val data = dataset[position]

        holder.itemView.apply {
            ivThumbnail.setImageResource(data.imageResourceId)
            tvTitle.text = data.title
        }
    }

    override fun getItemCount(): Int = dataset.size
}