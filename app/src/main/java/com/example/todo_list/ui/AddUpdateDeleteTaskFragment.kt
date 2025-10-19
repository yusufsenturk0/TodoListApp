package com.example.todo_list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.todo_list.Data.Task
import com.example.todo_list.Data.TaskDAO
import com.example.todo_list.Data.TaskDatabase
import com.example.todo_list.databinding.FragmentAddUpdateDeleteTaskBinding
import com.example.todo_list.helper.AlarmHelper
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.SimpleDateFormat
import java.util.*

class AddUpdateDeleteTaskFragment : Fragment() {

    private var _binding: FragmentAddUpdateDeleteTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: TaskDatabase
    private lateinit var taskDao: TaskDAO

    private val mDisposable = CompositeDisposable()

    private var updateId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = TaskDatabase.getInstance(requireContext())
        taskDao = db.taskDao()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonAdd.setOnClickListener { add() }
        binding.buttonUpdate.setOnClickListener { update() }

        arguments?.let {
            val args = AddUpdateDeleteTaskFragmentArgs.fromBundle(requireArguments())
            val bilgi = args.bilgi
            updateId = args.id

            if (bilgi == "new") {
                binding.buttonAdd.isVisible = true
                binding.buttonUpdate.isVisible = false
                binding.textViewTitle.setText("")
                binding.textViewDescription.setText("")
            } else {
                binding.buttonAdd.isVisible = false
                binding.buttonUpdate.isVisible = true
                binding.buttonUpdate.isEnabled = true

                mDisposable.add(
                    taskDao.findById(updateId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleResponseUpdate)
                )
            }
        }

        // Hatırlatıcı switch görünürlüğü
        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutDate.isVisible = isChecked
            binding.layoutTime.isVisible = isChecked
        }

        // Date picker
        binding.editTextDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Tarih Seç")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.show(parentFragmentManager, "DATE_PICKER")

            datePicker.addOnPositiveButtonClickListener { selection ->
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                binding.editTextDate.setText(sdf.format(Date(selection)))
            }
        }

        // Time picker
        binding.editTextTime.setOnClickListener {
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setTitleText("Saat Seç")
                .build()

            timePicker.show(parentFragmentManager, "TIME_PICKER")

            timePicker.addOnPositiveButtonClickListener {
                val hour = if (timePicker.hour < 10) "0${timePicker.hour}" else "${timePicker.hour}"
                val minute = if (timePicker.minute < 10) "0${timePicker.minute}" else "${timePicker.minute}"
                binding.editTextTime.setText("$hour:$minute")
            }
        }
    }

    private fun add() {
        val title = binding.textViewTitle.text.toString().trim()
        val description = binding.textViewDescription.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen görev başlığı ve açıklama girin", Toast.LENGTH_SHORT).show()
            return
        }

        var reminderDate: Long? = null
        var reminderTime: String? = null

        if (binding.switchReminder.isChecked) {
            val dateText = binding.editTextDate.text.toString().trim()
            val timeText = binding.editTextTime.text.toString().trim()

            if (dateText.isEmpty() || timeText.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen tarih ve saat seçin", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                reminderDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateText)?.time
                reminderTime = timeText

                // Geçmiş tarih kontrolü
                val reminderMillis = combineDateAndTime(reminderDate!!, reminderTime)
                if (reminderMillis <= System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), "Hatırlatıcı zamanı gelecekte olmalı", Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Tarih formatı hatalı", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val task = Task(
            title = title,
            description = description,
            reminderDate = reminderDate,
            reminderTime = reminderTime
        )

        mDisposable.add(
            taskDao.insert(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { newId ->
                        if (task.reminderTime != null && task.reminderDate != null) {
                            val reminderMillis = combineDateAndTime(task.reminderDate, task.reminderTime)
                            AlarmHelper.setReminder(
                                requireContext(),
                                newId.toInt(),
                                reminderMillis,
                                task.title,
                                task.description
                            )
                        }
                        Toast.makeText(requireContext(), "Görev eklendi", Toast.LENGTH_SHORT).show()
                        requireView().findNavController().popBackStack()
                    },
                    { error ->
                        error.printStackTrace()
                        Toast.makeText(requireContext(), "Kaydetme hatası!", Toast.LENGTH_SHORT).show()
                    }
                )
        )
    }

    private fun update() {
        val title = binding.textViewTitle.text.toString().trim()
        val description = binding.textViewDescription.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen görev başlığı ve açıklama girin", Toast.LENGTH_SHORT).show()
            return
        }

        var reminderDate: Long? = null
        var reminderTime: String? = null

        if (binding.switchReminder.isChecked) {
            val dateText = binding.editTextDate.text.toString().trim()
            val timeText = binding.editTextTime.text.toString().trim()

            if (dateText.isEmpty() || timeText.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen tarih ve saat seçin", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                reminderDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateText)?.time
                reminderTime = timeText

                // Geçmiş tarih kontrolü
                val reminderMillis = combineDateAndTime(reminderDate!!, reminderTime)
                if (reminderMillis <= System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), "Hatırlatıcı zamanı gelecekte olmalı", Toast.LENGTH_SHORT).show()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Tarih formatı hatalı", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val task = Task(
            title = title,
            description = description,
            reminderDate = reminderDate,
            reminderTime = reminderTime
        )
        task.id = updateId

        mDisposable.add(
            taskDao.update(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        if (task.reminderTime != null && task.reminderDate != null) {
                            val reminderMillis = combineDateAndTime(task.reminderDate, task.reminderTime)
                            AlarmHelper.setReminder(
                                requireContext(),
                                task.id,
                                reminderMillis,
                                task.title,
                                task.description
                            )
                        } else {
                            AlarmHelper.cancelReminder(requireContext(), task.id)
                        }
                        Toast.makeText(requireContext(), "Görev güncellendi", Toast.LENGTH_SHORT).show()
                        requireView().findNavController().popBackStack()
                    },
                    { error ->
                        error.printStackTrace()
                        Toast.makeText(requireContext(), "Güncelleme hatası!", Toast.LENGTH_SHORT).show()
                    }
                )
        )
    }

    private fun combineDateAndTime(dateMillis: Long, timeString: String): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val parts = timeString.split(":")
        calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        calendar.set(Calendar.MINUTE, parts[1].toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun handleResponseUpdate(task: Task) {
        if (_binding == null) return

        binding.textViewTitle.setText(task.title)
        binding.textViewDescription.setText(task.description)

        if (task.reminderDate != null && task.reminderTime != null) {
            binding.switchReminder.isChecked = true
            binding.editTextDate.setText(
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(task.reminderDate))
            )
            binding.editTextTime.setText(task.reminderTime)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddUpdateDeleteTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDisposable.clear()
        _binding = null
    }
}