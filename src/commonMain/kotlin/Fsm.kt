package com.github.tomoya0x00.statek

interface BaseState
interface BaseEvent

private typealias EventName = String
private typealias Action = () -> Unit
private typealias Entry<T> = T.() -> Unit
private typealias Exit<T> = T.() -> Unit
private typealias Guard<S, E> = S.(E) -> Boolean
private typealias EdgeAction<S, E> = S.(E) -> Unit

@DslMarker
annotation class FsmDsl

class StateMachine<T>(
    private var fsmContext: FsmContext<T>,
    private val transitionMap: Map<T, List<Transition<T>>>,
    private val stateToRootMap: Map<T, LinkedHashMap<T, StateDetail<T>>>

) where T : Enum<T>, T : BaseState {
    val state: T
        get() = fsmContext.state

    fun isStateOfChildOf(state: T): Boolean =
        this.state != state && (stateToRootMap[this.state]?.containsKey(state) ?: false)

    fun dispatch(event: BaseEvent): T {
        return fsmContext.dispatch(event, transitionMap)
    }

    @FsmDsl
    class Builder<T>(private val initial: T)
            where T : Enum<T>, T : BaseState {
        private val fsmContext = FsmContext(initial)
        private val rootChildren: MutableList<StateDetail<T>> = mutableListOf()

        private val allStateDetails: List<StateDetail<T>>
            get() = rootChildren.map { it.allStateDetails }.flatten()

        fun state(
            state: T,
            entry: Entry<T> = {},
            exit: Exit<T> = {},
            init: StateDetail<T>.() -> Unit = {}
        ) = this.rootChildren.add(StateDetail(
            parent = null,
            state = state,
            entry = { entry.invoke(state) },
            exit = { exit.invoke(state) }
        ).apply(init))

        fun build(): StateMachine<T> {
            val allStateDetails = allStateDetails

            val stateToRootMap = mutableMapOf<T, LinkedHashMap<T, StateDetail<T>>>()
            allStateDetails.forEach { stateDetail ->
                if (stateToRootMap.containsKey(stateDetail.state)) {
                    throw Exception("duplicate state(${stateDetail.state.enumNameOrClassName()}) found!")
                }

                stateToRootMap[stateDetail.state] =
                    generateSequence(stateDetail) { it.parent }.fold(LinkedHashMap()) { acc, parent ->
                        acc[parent.state] = parent
                        acc
                    }
            }

            val transitionMap = mutableMapOf<T, MutableList<Transition<T>>>()

            // sorting by depth to inherit parent's edges
            allStateDetails.sortedBy { stateToRootMap[it.state]?.size }.forEach { stateDetail ->
                transitionMap[stateDetail.state] = mutableListOf()

                val stateToRoot = stateToRootMap[stateDetail.state] ?: return@forEach

                // translate my edges to transitions
                stateDetail.edges.forEach { edge ->
                    val nextToRoot = stateToRootMap[edge.next]
                        ?: throw Exception("(${stateDetail.state.enumNameOrClassName()}) to root was not found!")

                    // excluding state included in both
                    val stateToNext = (stateToRoot.entries + nextToRoot.entries.reversed())
                        .asSequence()
                        .filterNot { stateToRoot.containsKey(it.key) and nextToRoot.containsKey(it.key) }

                    val actions = stateToNext
                        .map { if (stateToRoot.containsKey(it.key)) it.value.exit else it.value.entry }
                        .toList()

                    transitionMap[stateDetail.state]?.add(
                        Transition(
                            event = edge.eventName,
                            guard = edge.guard,
                            next = edge.next,
                            edgeAction = edge.action,
                            actions = actions
                        )
                    )
                }

                // translate my parent's edges to my transitions
                val parentState = stateDetail.parent?.state ?: return@forEach
                transitionMap[parentState]?.let { parentTransitions ->
                    transitionMap[stateDetail.state]?.addAll(
                        parentTransitions.map { original ->
                            original.copy(
                                actions = listOf(stateDetail.exit) + original.actions
                            )
                        }
                    )
                }
            }

            // execute entry actions of initial state
            val rootToInitial = stateToRootMap[initial]?.values?.reversed() ?: emptyList()
            rootToInitial.forEach { it.entry.invoke() }

            return StateMachine(fsmContext, transitionMap, stateToRootMap)
        }

        override fun toString(): String {
            return "StateMachine\n" +
                    rootChildren.joinToString("\n") { it.toString() }.prependIndent("  ")
        }
    }

    data class Transition<T>(
        val event: EventName,
        val guard: ((BaseEvent) -> Boolean)? = null,
        val next: T,
        val edgeAction: ((BaseEvent) -> Unit)? = null,
        val actions: List<Action> = listOf()
    ) where T : Enum<T>, T : BaseState
}

@FsmDsl
class StateDetail<T>(
    val parent: StateDetail<T>?,
    val state: T,
    val entry: Action = {},
    val exit: Action = {}
) where T : Enum<T>, T : BaseState {
    private val children: MutableList<StateDetail<T>> = mutableListOf()
    val edges: MutableList<Edge<T>> = mutableListOf()

    val allStateDetails: List<StateDetail<T>>
        get() = children.map { it.allStateDetails }.flatten() + this

    fun state(
        state: T,
        entry: Entry<T> = {},
        exit: Exit<T> = {},
        init: StateDetail<T>.() -> Unit = {}
    ) = this.children.add(StateDetail(
        parent = this,
        state = state,
        entry = { entry.invoke(state) },
        exit = { exit.invoke(state) }
    ).apply(init))

    inline fun <reified R : BaseEvent> edge(
        next: T = this.state,
        noinline guard: Guard<T, R>? = null,
        noinline action: EdgeAction<T, R>? = null
    ) = this.edges.add(Edge(
        eventName = R::class.simpleName ?: "",
        guard = guard?.let { { event: BaseEvent -> it.invoke(state, event as R) } },
        next = next,
        action = action?.let { { event: BaseEvent -> it.invoke(state, event as R) } }
    ))

    override fun toString(): String {
        return "${state.enumNameOrClassName()}\n" +
                edges.joinToString("\n") { it.toString() }.prependIndent("  ") + "\n" +
                children.joinToString("\n") { it.toString() }.prependIndent("  ")
    }
}

class Edge<T>(
    val eventName: EventName,
    val next: T,
    val guard: ((BaseEvent) -> Boolean)? = null,
    val action: ((BaseEvent) -> Unit)? = null
) where T : Enum<T>, T : BaseState {
    override fun toString(): String {
        return "--> ${next.name} : $eventName"
    }
}

private fun Any.enumNameOrClassName(): String =
    if (this is Enum<*>) this.name else this::class.simpleName ?: ""

class FsmContext<T>(initial: T)
        where T : Enum<T>, T : BaseState {

    var state: T = initial
        private set

    fun dispatch(event: BaseEvent, transitionMap: Map<T, List<StateMachine.Transition<T>>>): T {
        val transition = transitionMap[state]?.let { transitions ->
            transitions.filter { it.event == event::class.simpleName }
                .firstOrNull { it.guard?.invoke(event) ?: true }
        }

        transition?.run {
            edgeAction?.invoke(event)
            actions.forEach { it.invoke() }
            state = next
        }

        return state
    }
}

fun <T> stateMachine(
    initial: T,
    init: StateMachine.Builder<T>.() -> Unit
): StateMachine<T> where T : Enum<T>, T : BaseState =
    StateMachine.Builder(initial = initial).apply(init).build()