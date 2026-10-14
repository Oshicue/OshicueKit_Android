package com.oshicue.kit.ui.common

import com.oshicue.kit.core.KitText

fun kitStandardContains(value: String, search: String): Boolean = KitText.standardContains(value, search)

fun kitStandardSort(left: String, right: String): Int = KitText.standardCompare(left, right)
