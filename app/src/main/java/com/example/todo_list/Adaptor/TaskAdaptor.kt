package com.example.todo_list.Adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.findNavController
import androidx.room.Room
import com.example.todo_list.Data.Task
import com.example.todo_list.Data.TaskDAO
import com.example.todo_list.Data.TaskDatabase
import com.example.todo_list.databinding.TaskItemRowBinding
import com.example.todo_list.ui.TaskListFragmentDirections
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskAdaptor(
    private val tasks: MutableList<Task> // artık değiştirilebilir
    ,private val onDeleteClick: (Task,Int) -> Unit
) : RecyclerView.Adapter<TaskAdaptor.TaskViewHolder>() {

    private lateinit var db : TaskDatabase
    private lateinit var taskDao : TaskDAO

    private val mDisposable = CompositeDisposable()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {

        db= Room.databaseBuilder(parent.context, TaskDatabase::class.java,"Tasks").build()
        taskDao=db.taskDao()

        return TaskViewHolder(
            TaskItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.textViewTaskTitle.text = task.title
        holder.binding.checkBoxTaskDone.isChecked = task.isCompleted

        // Başlık için strike-through ilk durum
        holder.binding.textViewTaskTitle.paintFlags = if (task.isCompleted)
            holder.binding.textViewTaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        else
            holder.binding.textViewTaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

        // Checkbox değiştiğinde strike-through uygula
        holder.binding.checkBoxTaskDone.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                holder.binding.textViewTaskTitle.paintFlags =
                    holder.binding.textViewTaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.binding.textViewTaskTitle.paintFlags =
                    holder.binding.textViewTaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            task.isCompleted = isChecked // Modeli de güncelliyoruz
        }

        // Silme butonu
        holder.binding.buttonDelete.setOnClickListener {
            onDeleteClick(task, position)
        }
        //Reminder varsa reminder icon olcak yoksa olmıcak
        if(task.reminderDate != null && task.reminderTime != null){
            holder.binding.iconReminder.visibility = View.VISIBLE
        }else{
            holder.binding.iconReminder.visibility = View.GONE
        }
        //Silme butonu


        // Düzenleme ve tıklama
        holder.binding.buttonEdit.setOnClickListener {
            println("position: $position")
            val action = TaskListFragmentDirections
                .actionTaskListFragmentToAddUpdateDeleteTaskFragment(bilgi = "update", id = tasks[position].id)
            it.findNavController().navigate(action)
        }
    }




    override fun getItemCount() = tasks.size

    fun removeTaskAt(position: Int) {
        tasks.removeAt(position)
        notifyItemRemoved(position)
    }

    class TaskViewHolder(val binding: TaskItemRowBinding) : RecyclerView.ViewHolder(binding.root)
}
