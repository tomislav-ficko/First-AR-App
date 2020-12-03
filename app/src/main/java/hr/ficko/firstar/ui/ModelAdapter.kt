package hr.ficko.firstar.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import hr.ficko.firstar.R
import hr.ficko.firstar.models.Model
import kotlinx.android.synthetic.main.item_model.view.*

const val SELECTED_MODEL_COLOR = Color.YELLOW
const val UNSELECTED_MODEL_COLOR = Color.LTGRAY

class ModelAdapter(val dataset: List<Model>) :
    RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {

    var selectedModelLiveData = MutableLiveData<Model>()
    private var selectedModelIndex = 0

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
        val currentModel = dataset[position]

        if (currentModelSelected(holder)) {
            setBackgroundForSelectedModel(holder)
            selectedModelLiveData.postValue(currentModel)
        } else {
            setBackgroundForUnselectedModel(holder)
        }

        holder.itemView.apply {
            ivThumbnail.setImageResource(currentModel.imageResourceId)
            tvTitle.text = currentModel.title

            setOnClickListener {
                selectModel(holder)
            }
        }
    }

    override fun getItemCount(): Int = dataset.size

    private fun selectModel(holder: ModelViewHolder) {
        if (anotherModelSelected(holder)) {
            setBackgroundForSelectedModel(holder)

            notifyItemChanged(selectedModelIndex)
            selectedModelIndex = holder.layoutPosition
            selectedModelLiveData.postValue(dataset[selectedModelIndex])
        }
    }

    private fun setBackgroundForSelectedModel(holder: ModelViewHolder) {
        holder.itemView.setBackgroundColor(SELECTED_MODEL_COLOR)
    }

    private fun setBackgroundForUnselectedModel(holder: ModelViewHolder) {
        holder.itemView.setBackgroundColor(UNSELECTED_MODEL_COLOR)
    }

    private fun currentModelSelected(holder: ModelViewHolder) =
        selectedModelIndex == holder.layoutPosition

    private fun anotherModelSelected(holder: ModelViewHolder) =
        selectedModelIndex != holder.layoutPosition
}
