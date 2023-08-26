package com.example.mytodo.model

enum class Priority(val value: Int) {
    IMPORTANT(1),
    NORMAL(2),
    UNIMPORTANT(3);

    companion object {
        fun fromInt(value: Int) = Priority.values().first { it.value == value }
    }
}