package com.rjvjha.android.employee.work

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rjvjha.android.employee.EmployeeApplication
import com.rjvjha.android.employee.data.model.Employee

class SaveEmployeeWorkerClass (private  val context:Context, workerParameters: WorkerParameters):Worker(context,workerParameters) {



    override fun doWork(): Result {
        val databaseService = (context.applicationContext as EmployeeApplication).databaseService
        val emp = Employee (14,"dummy@outlook.com","Adam","Jones","")
        databaseService.employeeDao().insert(emp)
        return Result.success()
    }


}