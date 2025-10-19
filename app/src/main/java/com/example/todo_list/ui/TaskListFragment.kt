package com.example.todo_list.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todo_list.Adaptor.TaskAdaptor
import com.example.todo_list.Data.Task
import com.example.todo_list.Data.TaskDAO
import com.example.todo_list.Data.TaskDatabase
import com.example.todo_list.databinding.FragmentTaskListBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class TaskListFragment : Fragment() {

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: TaskDatabase
    private lateinit var taskDao: TaskDAO

    private val mDisposable = CompositeDisposable()

    private lateinit var adaptor: TaskAdaptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = TaskDatabase.getInstance(requireContext())
        taskDao = db.taskDao()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.floatingActionButton.setOnClickListener { addTask(it) }
        binding.recyclerViewListTask.layoutManager = LinearLayoutManager(requireContext())

        getAllData()
    }

    private fun addTask(view: View) {
        val action = TaskListFragmentDirections.actionTaskListFragmentToAddUpdateDeleteTaskFragment(
            bilgi = "new",
            id = 0
        )
        view.findNavController().navigate(action)
    }

    private fun getAllData() {
        mDisposable.add(
            taskDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleResponse)
        )
    }

    private fun handleResponse(tasks: List<Task>) {
        adaptor = TaskAdaptor(
            tasks = tasks.toMutableList(),
            onDeleteClick = { task, position ->
                deleteTask(task, position)
            },
            onCheckboxChanged = { task, isChecked ->
                updateTaskStatus(task, isChecked)
            }
        )
        binding.recyclerViewListTask.adapter = adaptor
    }

    private fun deleteTask(task: Task, position: Int) {
        mDisposable.add(
            taskDao.delete(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        adaptor.removeTaskAt(position)
                    },
                    { error ->
                        error.printStackTrace()
                    }
                )
        )
    }

    private fun updateTaskStatus(task: Task, isChecked: Boolean) {
        task.isCompleted = isChecked
        mDisposable.add(
            taskDao.update(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        // Güncelleme başarılı
                    },
                    { error ->
                        error.printStackTrace()
                    }
                )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDisposable.clear()
        _binding = null
    }
}