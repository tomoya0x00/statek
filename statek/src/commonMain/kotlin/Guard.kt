package com.github.tomoya0x00.statek

class Guard<T, R : BaseEvent>(
    val description: String,
    val guardStatement: GuardStatement<T, R>
) where T : Enum<T>, T : BaseState
