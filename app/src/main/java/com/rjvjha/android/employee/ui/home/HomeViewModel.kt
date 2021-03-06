package com.rjvjha.android.employee.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.rjvjha.android.employee.data.Repository.EmployeeRepository
import com.rjvjha.android.employee.data.model.Employee
import com.rjvjha.android.employee.data.remote.response.EmployeeListResponse
import com.rjvjha.android.employee.ui.base.BaseViewModel
import com.rjvjha.android.employee.utils.common.OrderUtils
import com.rjvjha.android.employee.utils.network.NetworkHelper
import com.rjvjha.android.employee.utils.common.Resource
import com.rjvjha.android.employee.utils.common.Status
import com.rjvjha.android.employee.utils.rx.SchedulerProvider
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction

class HomeViewModel(
    schedulerProvider: SchedulerProvider,
    compositeDisposable: CompositeDisposable,
    networkHelper: NetworkHelper,
    private val employeeRepository: EmployeeRepository
    ): BaseViewModel(schedulerProvider,compositeDisposable, networkHelper) {

    companion object{
        const val TAG = "HomeViewModel"
        const val ORDER_BY_NAME = 101
        const val ORDER_BY_ID = 102
    }

    var currentOrder = -1

    val empList: MutableLiveData<Resource<List<Employee>>> = MutableLiveData()

    val empListSorted = MediatorLiveData<List<Employee>>()

    private val sortedListByName =  MutableLiveData<List<Employee>>()

    private val sortedListById =  MutableLiveData<List<Employee>>()

    fun getEmpList(): LiveData<List<Employee>> =
            Transformations.map(empList) { it.data}

    fun isEmpListFetching(): LiveData<Boolean> =
        Transformations.map(empList) { it.status == Status.LOADING }


    fun rearrangeOrder(order :Int) = when (order) {
            ORDER_BY_ID -> { val list = OrderUtils.sortById(empList.value?.data)
                            sortedListById.postValue(list) }
            ORDER_BY_NAME -> { val list = OrderUtils.sortByName(empList.value?.data)
                                sortedListByName.postValue(list) }
        else -> {}
    }.also {
        currentOrder = order }


    override fun onCreate() {

        empListSorted.addSource(sortedListById){result ->
                result.let { empListSorted.value = (it) }
        }

        empListSorted.addSource(sortedListByName){result ->
            result.let { empListSorted.value = (it) }
        }

        if (empList.value == null && checkInternetConnection()) {
            empList.postValue(Resource.loading())
            compositeDisposable.add(
                Observable.zip(employeeRepository.fetchEmployeeList(1),
                    employeeRepository.fetchEmployeeList(2),
                BiFunction<EmployeeListResponse, EmployeeListResponse,List<Employee>>{
                    page1data,page2data -> return@BiFunction page1data.data.plus(page2data.data)
                }).subscribeOn(schedulerProvider.io())
                    .subscribe(
                        {
                            empList.postValue(Resource.success(it)) },
                        {
                            handleNetworkError(it)
                            empList.postValue(Resource.error())
                        })
            )
        } else if (empList.value == null && !networkHelper.isNetworkConnected()){
            // no internet connection
            empList.postValue(Resource.loading())
            compositeDisposable.add(
                employeeRepository.fetchEmployeeListFromLocal()
                    .observeOn(schedulerProvider.io())
                    .subscribeOn(schedulerProvider.io())
                    .subscribe({
                        empList.postValue(Resource.success(it))
                    },{
                        empList.postValue(Resource.error())
                    })
            )

        }



    }

}