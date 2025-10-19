package com.example.todo_list.Adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.findNavController
import com.example.todo_list.Data.Task
import com.example.todo_list.databinding.TaskItemRowBinding
import com.example.todo_list.ui.TaskListFragmentDirections

class TaskAdaptor(
    private val tasks: MutableList<Task>,
    private val onDeleteClick: (Task, Int) -> Unit,
    private val onCheckboxChanged: (Task, Boolean) -> Unit
) : RecyclerView.Adapter<TaskAdaptor.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder(
            TaskItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task, position, onDeleteClick, onCheckboxChanged)
    }

    override fun getItemCount() = tasks.size

    fun removeTaskAt(position: Int) {
        tasks.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateTaskAt(position: Int, task: Task) {
        tasks[position] = task
        notifyItemChanged(position)
    }

    class TaskViewHolder(val binding: TaskItemRowBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            task: Task,
            position: Int,
            onDeleteClick: (Task, Int) -> Unit,
            onCheckboxChanged: (Task, Boolean) -> Unit
        ) {
            binding.textViewTaskTitle.text = task.title

            // Listener'ı kaldır (önceki bind'dan kalan listener'ları temizle)
            binding.checkBoxTaskDone.setOnCheckedChangeListener(null)
            binding.checkBoxTaskDone.isChecked = task.isCompleted

            // Strike-through durumu
            updateStrikeThrough(task.isCompleted)

            // Yeni listener ekle
            binding.checkBoxTaskDone.setOnCheckedChangeListener { _, isChecked ->
                updateStrikeThrough(isChecked)
                onCheckboxChanged(task, isChecked)
            }

            // Silme butonu
            binding.buttonDelete.setOnClickListener {
                onDeleteClick(task, position)
            }

            // Reminder icon görünürlüğü
            binding.iconReminder.visibility = if (task.reminderDate != null && task.reminderTime != null) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Düzenleme butonu
            binding.buttonEdit.setOnClickListener {
                val action = TaskListFragmentDirections
                    .actionTaskListFragmentToAddUpdateDeleteTaskFragment(bilgi = "update", id = task.id)
                it.findNavController().navigate(action)
            }
        }

        private fun updateStrikeThrough(isCompleted: Boolean) {
            binding.textViewTaskTitle.paintFlags = if (isCompleted) {
                binding.textViewTaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.textViewTaskTitle.paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
        }
    }
}