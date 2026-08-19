package io.github.allexnik

import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.io.readExcel
import java.io.File

fun main(args: Array<String>) {
    val df = DataFrame.readExcel(File(args.single()))
    println(df)
}