package com.example.medicationreminder.domain

enum class Zodiac(val label: String) {
    RAT("鼠"), OX("牛"), TIGER("虎"), RABBIT("兔"),
    DRAGON("龙"), SNAKE("蛇"), HORSE("马"), GOAT("羊"),
    MONKEY("猴"), ROOSTER("鸡"), DOG("狗"), PIG("猪");

    companion object {
        fun fromId(id: String?): Zodiac = entries.firstOrNull { it.name == id } ?: RABBIT
    }
}
