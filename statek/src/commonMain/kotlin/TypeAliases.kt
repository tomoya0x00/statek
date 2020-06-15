package com.github.tomoya0x00.statek

internal typealias EventName = String
internal typealias ActionStatement = () -> Unit
internal typealias EntryStatement<T> = T.() -> Unit
internal typealias ExitStatement<T> = T.() -> Unit
internal typealias GuardStatement<S, E> = S.(E) -> Boolean
internal typealias EdgeActionStatement<S, E> = S.(E) -> Unit
