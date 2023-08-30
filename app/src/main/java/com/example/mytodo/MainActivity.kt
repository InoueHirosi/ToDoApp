package com.example.mytodo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.mytodo.model.Priority
import com.example.mytodo.room.Todo
import com.example.mytodo.room.TodoDAO
import com.example.mytodo.room.TodoDatabase
import com.example.mytodo.ui.DatePick
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    // ToDo追加用の画面を用意（未初期化）
    private lateinit var addTodoView: View

    // Room Databaseを用意（未初期化）
    private lateinit var db: TodoDatabase
    private lateinit var dao: TodoDAO

    // 表示するリストを用意（今は空）
    private var addList = ArrayList<Todo>()

    // ソート用のフラグを用意
    private var sortedByDeadline = false
    private var sortedByPriority = false

    // RecyclerViewを宣言
    private lateinit var recyclerView: RecyclerView

    // RecyclerViewのAdapterを用意（未初期化）
    private lateinit var recyclerAdapter: RecyclerAdapter

    // 期限日の優先度のための数値
    private var deadlineNum: Int = 0

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ヘッダータイトルを非表示
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)

        // Viewをセット
        setContentView(R.layout.activity_main)

        // View要素を取得
        val btnAdd: Button = findViewById(R.id.btnAdd)
        recyclerView = findViewById(R.id.rv)

        // コンテンツを変更してもRecyclerViewのレイアウトサイズを変更しない場合はこの設定を使用してパフォーマンスを向上
        recyclerView.setHasFixedSize(true)

        // レイアウトマネージャーで列数を2列に指定
        recyclerView.layoutManager = GridLayoutManager(this, 2, RecyclerView.VERTICAL, false)
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)

        // Roomオブジェクトを初期化
        db = Room.databaseBuilder(
            this, TodoDatabase::class.java, "todo.db"
        ).build()
        dao = db.todoDAO()

        // 初期表示時にToDoレコードを全件取得
        GlobalScope.launch {
            // RoomからTodoリストを取得
            addList = dao.getAll() as ArrayList<Todo>

            // recyclerAdapterの初期化
            recyclerAdapter = RecyclerAdapter(addList)

            recyclerAdapter.setOnCellClickListener(
                // ToDoカードビューのクリック処理
                object : RecyclerAdapter.OnCellClickListener {
                    override fun onItemClick(position: Int) {
                        generateAlertDialog(true, position)
                    }
                })
            // RecyclerViewにAdapterをセット
            recyclerView.adapter = recyclerAdapter
        }

        // 追加ボタン押下時にAlertDialogを表示する
        btnAdd.setOnClickListener {
            generateAlertDialog(false, 0)
        }

        // 表示しているアイテムがタッチされた時の設定
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            // アイテムをドラッグできる方向を指定
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            // アイテムをスワイプできる方向を指定
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            // アイテムドラッグ時の挙動を設定
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                // アイテム位置の入れ替えを行う
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                recyclerAdapter.notifyItemMoved(fromPos, toPos)
                // Roomの情報を更新
                val fromData = addList[fromPos]
                fromData.displayOrder = toPos
                val toData = addList[toPos]
                toData.displayOrder = fromPos
                GlobalScope.launch {
                    dao.upsert(fromData)
                    dao.upsert(toData)
                }
                return true
            }

            // アイテムスワイプ時の挙動を設定
            @SuppressLint("NotifyDataSetChanged")
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // アイテムスワイプ時にAlertDialogを表示
                android.app.AlertDialog.Builder(this@MainActivity)
                    // AlertDialogのタイトルを設定
                    .setTitle(R.string.removeTitle)
                    // AlertDialogのyesボタンを設定
                    .setPositiveButton(R.string.yes) { arg0: DialogInterface, _: Int ->
                        try {
                            // AlertDialogを非表示
                            arg0.dismiss()
                            // Roomから削除
                            val todo = addList[viewHolder.adapterPosition]
                            val id =
                                todo.title.hashCode() / 4 - todo.detail.hashCode() / 4 + todo.deadline.hashCode() / 4 - todo.priority.hashCode() / 4
                            GlobalScope.launch {
                                dao.delete(id)
                            }
                            // UIスレッドで実行
                            runOnUiThread {
                                // スワイプされたアイテムを削除
                                addList.removeAt(viewHolder.adapterPosition)
                                // 表示するリストを更新(アイテムが削除されたことを通知)
                                recyclerAdapter.notifyItemRemoved(viewHolder.adapterPosition)
                            }
                        } catch (ignored: Exception) {
                        }
                    }.setNegativeButton(R.string.no) { _: DialogInterface, _: Int ->
                        // 表示するリストを更新(アイテムが変更されたことを通知)
                        recyclerAdapter.notifyDataSetChanged()
                    }
                    // AlertDialogを表示
                    .show()
            }
        })

        // 表示しているアイテムがタッチされた時の設定をリストに適用
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun generateAlertDialog(isUpdate: Boolean, index: Int) {
        val nullParent: ViewGroup? = null
        addTodoView = layoutInflater.inflate(R.layout.add_todo, nullParent)
        // AlertDialog内の表示項目を取得
        val txtTitle: EditText = addTodoView.findViewById(R.id.title)
        val txtDetail: EditText = addTodoView.findViewById(R.id.detail)
        val btnDeadline: Button = addTodoView.findViewById(R.id.deadline)
        val priorityButtonGroup: RadioGroup = addTodoView.findViewById(R.id.radioGroup)
        if (isUpdate) {
            // 編集時
            txtTitle.setText(addList[index].title)
            txtDetail.setText(addList[index].detail)
            btnDeadline.text = addList[index].deadline
            when (addList[index].priority) {
                Priority.IMPORTANT -> {
                    priorityButtonGroup.check(R.id.RadioButtonB1)
                }

                Priority.NORMAL -> {
                    priorityButtonGroup.check(R.id.RadioButtonB2)
                }

                Priority.UNIMPORTANT -> {
                    priorityButtonGroup.check(R.id.RadioButtonB3)
                }
            }
        } else {
            // 新規登録時
            priorityButtonGroup.check(R.id.RadioButtonB3)
        }
        // AlertDialogを生成
        android.app.AlertDialog.Builder(this)
            // AlertDialogのタイトルを設定
            .setTitle(R.string.addTitle)
            // AlertDialogの表示項目を設定
            .setView(addTodoView)
            // AlertDialogのyesボタンを設定し、押下時の挙動を記述
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                if (isUpdate) {
                    // 編集時
                    val id = addList[index].id
                    val displayOrder = addList[index].displayOrder
                    // 入力内容を取得
                    val title = txtTitle.text.toString()
                    val detail = txtDetail.text.toString()
                    val deadline = btnDeadline.text.toString()
                    val deadlineNum = if (deadline == R.string.deadline.toString()) Int.MAX_VALUE else deadlineNum
                    val priority = when (priorityButtonGroup.checkedRadioButtonId) {
                        R.id.RadioButtonB1 -> Priority.IMPORTANT
                        R.id.RadioButtonB2 -> Priority.NORMAL
                        R.id.RadioButtonB3 -> Priority.UNIMPORTANT
                        else -> Priority.IMPORTANT
                    }
                    updateTodoData(
                        Todo(
                            id, displayOrder, title, detail, deadline, deadlineNum, priority
                        ), index
                    )
                } else {
                    // 新規登録時
                    // 入力内容を取得
                    val title = txtTitle.text.toString()
                    val detail = txtDetail.text.toString()
                    val deadline = btnDeadline.text.toString()
                    val deadlineNum = if (deadline == R.string.deadline.toString()) Int.MAX_VALUE else deadlineNum
                    val priority = when (priorityButtonGroup.checkedRadioButtonId) {
                        R.id.RadioButtonB1 -> Priority.IMPORTANT
                        R.id.RadioButtonB2 -> Priority.NORMAL
                        R.id.RadioButtonB3 -> Priority.UNIMPORTANT
                        else -> Priority.IMPORTANT
                    }
                    val id =
                        title.hashCode() / 4 - detail.hashCode() / 4 + deadline.hashCode() / 4 - priority.hashCode() / 4
                    // ToDoを生成
                    insertTodoData(
                        Todo(
                            id, addList.size, title, detail, deadline, deadlineNum, priority
                        )
                    )
                }
            }
            // AlertDialogのnoボタンを設定
            .setNegativeButton(R.string.no, null)
            // AlertDialogを表示
            .show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun insertTodoData(data: Todo) {
        // Todoアイテムの重複をチェック
        if (addList.stream().anyMatch { e ->
                e.title == data.title && e.detail == data.detail && e.deadline == data.deadline && e.priority == data.priority
            }) {
            Toast.makeText(
                this, R.string.duplicateTitle, Toast.LENGTH_SHORT
            ).show()
            return
        }
        // 表示するリストの最後尾に追加
        addList.add(data)
        // 表示するリストを更新(アイテムが挿入されたことを通知)
        recyclerAdapter.notifyItemInserted(addList.size - 1)
        GlobalScope.launch {
            // Roomに追加
            dao.upsert(data)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateTodoData(data: Todo, index: Int) {
        // Todoアイテムの重複をチェック
        if (addList.indexOf(data) != index && addList.stream().anyMatch { e ->
                e.title == data.title && e.detail == data.detail && e.deadline == data.deadline && e.priority == data.priority
            }) {
            Toast.makeText(
                this, R.string.duplicateTitle, Toast.LENGTH_SHORT
            ).show()
            return
        }
        // 表示するリストの更新
        addList[index] = data
        // 表示するリストを更新(アイテムが更新されたことを通知)
        recyclerAdapter.notifyItemChanged(index)
        GlobalScope.launch {
            // Roomに追加
            dao.upsert(data)
        }
    }

    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        // カレンダー入力時
        val dateFormat = DateTimeFormatter.ofPattern("uu/MM/dd(E)")
        val deadline: LocalDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
        val period: Period = Period.between(LocalDate.now(), deadline)
        val duration: Duration = Duration.between(
            LocalDate.now().atTime(0, 0, 0), deadline.atTime(0, 0, 0)
        )
        val remainYears = if (period.years != 0) period.years + R.string.yearUnit else ""
        val remainMonths = if (period.months != 0) period.months + R.string.monthUnit else ""
        val txtDeadline =
            if (period.isNegative) R.string.expired else dateFormat.format(deadline) + "\n" + R.string.remain + remainYears + remainMonths + period.days + R.string.dayUnit
        addTodoView.findViewById<Button>(R.id.deadline).text = txtDeadline.toString()
        deadlineNum = duration.toDays().toInt()
    }

    fun showDatePickerDialog(@Suppress("UNUSED_PARAMETER") ignoredV: View) {
        // カレンダー表示
        val newFragment = DatePick()
        newFragment.show(supportFragmentManager, "datePicker")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun orderByPriority(@Suppress("UNUSED_PARAMETER") ignoredV: View) {
        // 優先度でソート
        sortedByPriority = !sortedByPriority
        if (addList.isEmpty()) {
            Toast.makeText(
                this, R.string.emptyTodoList, Toast.LENGTH_SHORT
            ).show()
        }
        addList.sortWith(compareBy<Todo> { it.priority }.thenBy { it.displayOrder })
        if (sortedByPriority) {
            addList.reverse()
        }
        // 表示するリストを更新(アイテムが変更されたことを通知)
        recyclerAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun orderByDeadline(@Suppress("UNUSED_PARAMETER") ignoredV: View) {
        // 期限日でソート
        sortedByDeadline = !sortedByDeadline
        if (addList.isEmpty()) {
            Toast.makeText(
                this, R.string.emptyTodoList, Toast.LENGTH_SHORT
            ).show()
        }
        addList.sortWith(compareBy<Todo> { it.deadlineNum }.thenBy { it.displayOrder })
        if (sortedByDeadline) {
            addList.reverse()
        }
        // 表示するリストを更新(アイテムが変更されたことを通知)
        recyclerAdapter.notifyDataSetChanged()
    }

}
