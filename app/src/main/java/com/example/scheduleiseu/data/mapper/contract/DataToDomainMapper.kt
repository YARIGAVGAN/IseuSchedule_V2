package com.example.scheduleiseu.data.mapper.contract

fun interface DataToDomainMapper<in IData, out ODomain> {
    fun map(input: IData): ODomain
}
