package com.example.mytodo.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.mytodo.MainActivity
import java.time.LocalDateTime

class DatePick : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val today: LocalDateTime = LocalDateTime.now()
        val year = today.year
        val month = today.monthValue - 1
        val day = today.dayOfMonth

        return DatePickerDialog(
            this.context as Context, activity as MainActivity?, year, month, day
        )
    }

    override fun onDateSet(
        view: android.widget.DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int
    ) {
    }
}