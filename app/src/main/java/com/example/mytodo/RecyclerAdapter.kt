package com.example.mytodo

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.mytodo.model.Priority
import com.example.mytodo.room.Todo

class RecyclerAdapter(private val todoList: ArrayList<Todo>) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolderItem>() {

    // リスナを格納する変数を定義（未初期化）
    private lateinit var listener: OnCellClickListener

    // インターフェースを作成
    interface OnCellClickListener {
        fun onItemClick(position: Int)
    }

    // リスナーをセット
    fun setOnCellClickListener(clickListener: OnCellClickListener) {
        listener = clickListener
    }

    // リストに表示するアイテムの表示内容
    inner class ViewHolderItem(v: View) : RecyclerView.ViewHolder(v) {
        val titleHolder: TextView = v.findViewById(R.id.title)
        val detailHolder: TextView = v.findViewById(R.id.detail)
        val deadlineHolder: TextView = v.findViewById(R.id.deadline)
        var cardView: CardView = v.findViewById(R.id.cardView)
    }

    // リストに表示するアイテムを生成
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.one_layout, parent, false)
        return ViewHolderItem(view)
    }

    // position番目のデータを表示
    override fun onBindViewHolder(holder: ViewHolderItem, position: Int) {
        val currentItem = todoList[position]
        holder.titleHolder.text = currentItem.title
        holder.detailHolder.text = currentItem.detail
        holder.deadlineHolder.text = currentItem.deadline
        if (currentItem.deadline == "期限切れ") {
            holder.deadlineHolder.setTextColor(Color.RED)
            holder.deadlineHolder.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.deadlineHolder.setTextColor(Color.GRAY)
            holder.deadlineHolder.typeface = Typeface.DEFAULT
        }
        val frameColor = when (currentItem.priority) {
            Priority.IMPORTANT -> Color.parseColor("#FF4444")
            Priority.NORMAL -> Color.parseColor("#FFBB33")
            else -> Color.parseColor("#33B5E5")
        }
        holder.cardView.setCardBackgroundColor(frameColor)
        holder.cardView.setOnClickListener {
            listener.onItemClick(position)
        }
    }

    // リストサイズを取得する用のメソッド
    override fun getItemCount(): Int {
        return todoList.size
    }
}