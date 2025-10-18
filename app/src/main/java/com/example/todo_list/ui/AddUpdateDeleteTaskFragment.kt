package com.example.todo_list.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.room.Room
import com.example.todo_list.Data.Task
import com.example.todo_list.Data.TaskDAO
import com.example.todo_list.Data.TaskDatabase
import com.example.todo_list.databinding.FragmentAddUpdateDeleteTaskBinding
import com.example.todo_list.util.AlarmPermissionHelper
import com.example.todo_list.util.ReminderHelper
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

    private lateinit var db : TaskDatabase
    private lateinit var taskDao : TaskDAO

    private val mDisposable = CompositeDisposable()

    private var updateId : Int =0




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db= Room.databaseBuilder(requireContext(), TaskDatabase::class.java,"Tasks").build()
        taskDao=db.taskDao()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Task işlemleri
        binding.buttonAdd.setOnClickListener { add(it) }
        binding.buttonUpdate.setOnClickListener { update(it) }

        arguments?.let {
            val args= AddUpdateDeleteTaskFragmentArgs.fromBundle(requireArguments())
            val bilgi= args.bilgi
            updateId= args.id

            if(bilgi=="new"){
                binding.buttonAdd.isVisible = true
                binding.buttonUpdate.isVisible = false
                binding.textViewTitle.setText("")
                binding.textViewDescription.setText("")
            } else {
                binding.buttonAdd.isVisible = false
                binding.buttonUpdate.isVisible = true
                binding.buttonUpdate.isEnabled=true


                mDisposable.add(
                    taskDao.getAll()
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
            val datePicker = MaterialDatePicker.Builder.datePicker().build()

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
                .build()

            timePicker.show(parentFragmentManager, "TIME_PICKER")

            timePicker.addOnPositiveButtonClickListener {
                val hour = if(timePicker.hour < 10) "0${timePicker.hour}" else "${timePicker.hour}"
                val minute = if(timePicker.minute < 10) "0${timePicker.minute}" else "${timePicker.minute}"
                binding.editTextTime.setText("$hour:$minute")
            }
        }
    }

    private fun add(view: View) {
        var reminderDate: Long? = null   // Epoch millis, null ise hatırlatıcı yok
        var reminderTime: String? = null // "HH:mm" formatında, null ise hatırlatıcı yok

        val title = binding.textViewTitle.text.toString().trim()
        val description = binding.textViewDescription.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen görev başlığı ve açıklama girin", Toast.LENGTH_SHORT).show()
            return
        }

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

        // 💾 Insert işlemi
        mDisposable.add(
            taskDao.insert(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        // Insert başarılı → alarm kur
                        if (!AlarmPermissionHelper.hasExactAlarmPermission(requireContext())) {
                            AlarmPermissionHelper.requestExactAlarmPermission(requireContext())
                        } else if (task.reminderDate != null && task.reminderTime != null) {
                            ReminderHelper.scheduleReminder(requireContext(), task)
                        }

                        handleResponseForInsert()
                    },
                    { error ->
                        error.printStackTrace()
                        Toast.makeText(requireContext(), "Kaydetme hatası!", Toast.LENGTH_SHORT).show()
                    }
                )
        )
    }



    private fun update(view: View) {
        var reminderDate: Long? = null
        var reminderTime: String? = null

        val title = binding.textViewTitle.text.toString().trim()
        val description = binding.textViewDescription.text.toString().trim()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Lütfen görev başlığı ve açıklama girin", Toast.LENGTH_SHORT).show()
            return
        }

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

        // 💾 Güncelleme işlemi
        mDisposable.add(
            taskDao.update(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        // Önce varsa eski alarmı iptal et
                        ReminderHelper.cancelReminder(requireContext(), task)

                        // Alarm izin kontrolü + kur
                        if (!AlarmPermissionHelper.hasExactAlarmPermission(requireContext())) {
                            AlarmPermissionHelper.requestExactAlarmPermission(requireContext())
                        } else if (task.reminderDate != null && task.reminderTime != null) {
                            ReminderHelper.scheduleReminder(requireContext(), task)
                        }

                        handleResponseForInsert()
                    },
                    { error ->
                        error.printStackTrace()
                        Toast.makeText(requireContext(), "Güncelleme hatası!", Toast.LENGTH_SHORT).show()
                    }
                )
        )
    }



    //eğer güncellemek için geldiysek verileri ekrana yükle
    private fun handleResponseUpdate(tasks: List<Task>){

        if(_binding==null) return
        for(task in tasks){
            if(updateId == task.id){
                binding.textViewTitle.setText(task.title)
                binding.textViewDescription.setText(task.description)
                if(task.reminderDate != null && task.reminderTime != null){
                    binding.switchReminder.isChecked = true
                    binding.editTextDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(task.reminderDate)))
                    binding.editTextTime.setText(task.reminderTime)

                }
            }
        }
    }

    private fun handleResponseForInsert(){
        requireView().findNavController().popBackStack()

    }



















    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddUpdateDeleteTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
