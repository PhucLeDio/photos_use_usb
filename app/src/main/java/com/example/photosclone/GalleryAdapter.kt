package com.example.photosclone

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.ViewSizeResolver

class GalleryAdapter(
    private var items: List<ListItem> = emptyList(),
    private val onImageClick: (Uri) -> Unit,
    private val onSelectionChange: (Boolean) -> Unit // Báo cho MainActivity biết khi nào bắt đầu/kết thúc chọn
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_IMAGE = 1
    }

    var isSelectionMode = false
        set(value) {
            field = value
            if (!value) {
                // Nếu thoát chế độ chọn, bỏ chọn tất cả ảnh
                items.filterIsInstance<ListItem.Image>().forEach { it.isSelected = false }
            }
            notifyDataSetChanged()
            onSelectionChange(value)
        }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.Image -> TYPE_IMAGE
        }
    }

    // THÊM HÀM NÀY VÀO ĐÂY:
    fun getSelectedUris(): List<Uri> {
        return items.filterIsInstance<ListItem.Image>()
            .filter { it.isSelected }
            .map { it.uri }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_date_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_thumbnail, parent, false)
            ImageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is ListItem.Header) {
            holder.bind(item)
        } else if (holder is ImageViewHolder && item is ListItem.Image) {
            holder.bind(item)
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<ListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        fun bind(item: ListItem.Header) {
            tvDate.text = item.date
        }
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgThumbnail: ImageView = itemView.findViewById(R.id.img_thumbnail)
        private val overlay: View = itemView.findViewById(R.id.view_selected_overlay)
        private val imgCheck: ImageView = itemView.findViewById(R.id.img_check)

        fun bind(item: ListItem.Image) {
            imgThumbnail.load(item.uri) { size(ViewSizeResolver(imgThumbnail)) }

            // Hiển thị trạng thái chọn
            overlay.visibility = if (item.isSelected) View.VISIBLE else View.GONE
            imgCheck.visibility = if (item.isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                if (isSelectionMode) {
                    item.isSelected = !item.isSelected
                    notifyItemChanged(adapterPosition)

                    // Nếu không còn ảnh nào được chọn, thoát chế độ chọn
                    if (items.filterIsInstance<ListItem.Image>().none { it.isSelected }) {
                        isSelectionMode = false
                    }
                } else {
                    onImageClick(item.uri)
                }
            }

            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    item.isSelected = true
                    notifyItemChanged(adapterPosition)
                }
                true
            }
        }
    }
}