import com.github.tomoya0x00.statek.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FsmTest {

    enum class MyState : BaseState {
        NOT_LOANED,
        ON_LOAN,
        LOCK,
        UNLOCK,
        EMERGENCY_LOCK
    }

    sealed class MyEvent : BaseEvent {
        object PressRental : MyEvent()
        object PressReturn : MyEvent()
        data class PressLock(val withReturn: Boolean) : MyEvent()
        object PressUnLock : MyEvent()
        object PressForceReturn : MyEvent()
        object PressEmergencyLock : MyEvent()
    }

    private val history = mutableListOf<String>()

    private fun smBuilder(initial: MyState): StateMachine.Builder<MyState> =
        stateMachineBuilder(initial = initial) {
            state(MyState.NOT_LOANED,
                entry = { history.add("in_NotLoaned") },
                exit = { history.add("out_NotLoaned") }
            ) {
                edge<MyEvent.PressRental>(MyState.LOCK)
            }
            state(MyState.ON_LOAN,
                entry = { history.add("in_OnLoan") },
                exit = { history.add("out_OnLoan") }
            ) {

                edge<MyEvent.PressForceReturn>(MyState.NOT_LOANED) {
                    history.add("action_PressForceReturn")
                }

                edge<MyEvent.PressEmergencyLock>(MyState.EMERGENCY_LOCK) {
                    history.add("action_PressEmergencyLock")
                }

                state(MyState.LOCK,
                    entry = { history.add("in_Lock") },
                    exit = { history.add("out_Lock") }
                ) {
                    edge<MyEvent.PressReturn>(MyState.NOT_LOANED)
                    edge<MyEvent.PressUnLock>(MyState.UNLOCK)
                    edge<MyEvent.PressLock> {
                        history.add("action_PressLock")
                    }

                    state(MyState.EMERGENCY_LOCK,
                        entry = { history.add("in_EmergencyLock") },
                        exit = { history.add("out_EmergencyLock") }
                    )
                }
                state(MyState.UNLOCK,
                    entry = { history.add("in_UnLock") },
                    exit = { history.add("out_UnLock") }
                ) {
                    edge<MyEvent.PressLock>(MyState.LOCK, guard = { !it.withReturn })
                    edge<MyEvent.PressLock>(MyState.NOT_LOANED, guard = { it.withReturn }) {
                        history.add("action_PressLockWithReturn")
                    }
                }
            }
        }

    private fun stateMachine(initial: MyState): StateMachine<MyState> =
        smBuilder(initial).build()

    @Test
    fun test() {
        val sm = stateMachine(MyState.NOT_LOANED)

        assertEquals(MyState.NOT_LOANED, sm.state)
        assertEquals(
            listOf(
                "in_NotLoaned"
            ),
            history
        )

        history.clear()
        assertEquals(MyState.LOCK, sm.dispatch(MyEvent.PressRental))
        assertEquals(
            listOf(
                "out_NotLoaned",
                "in_OnLoan",
                "in_Lock"
            ),
            history
        )

        assertTrue(sm.isStateOfChildOf(MyState.ON_LOAN))
        assertFalse(sm.isStateOfChildOf(MyState.NOT_LOANED))
        assertFalse(sm.isStateOfChildOf(MyState.LOCK))

        history.clear()
        assertEquals(MyState.LOCK, sm.dispatch(MyEvent.PressLock(withReturn = false)))
        assertEquals(
            listOf(
                "action_PressLock"
            ),
            history
        )

        history.clear()
        assertEquals(MyState.UNLOCK, sm.dispatch(MyEvent.PressUnLock))
        assertEquals(
            listOf(
                "out_Lock",
                "in_UnLock"
            ),
            history
        )
        assertTrue(sm.isStateOfChildOf(MyState.ON_LOAN))
        assertFalse(sm.isStateOfChildOf(MyState.NOT_LOANED))
        assertFalse(sm.isStateOfChildOf(MyState.UNLOCK))

        history.clear()
        assertEquals(MyState.LOCK, sm.dispatch(MyEvent.PressLock(withReturn = false)))
        assertEquals(
            listOf(
                "out_UnLock",
                "in_Lock"
            ),
            history
        )

        history.clear()
        assertEquals(MyState.UNLOCK, sm.dispatch(MyEvent.PressUnLock))
        assertEquals(
            listOf(
                "out_Lock",
                "in_UnLock"
            ),
            history
        )

        history.clear()
        assertEquals(MyState.NOT_LOANED, sm.dispatch(MyEvent.PressLock(withReturn = true)))
        assertEquals(
            listOf(
                "action_PressLockWithReturn",
                "out_UnLock",
                "out_OnLoan",
                "in_NotLoaned"
            ),
            history
        )
        assertFalse(sm.isStateOfChildOf(MyState.NOT_LOANED))
        assertFalse(sm.isStateOfChildOf(MyState.UNLOCK))
    }

    @Test
    fun test_ChildStateInheritsEdgesOfParent_WithEntryChildState() {
        val sm = stateMachine(MyState.LOCK)

        history.clear()
        assertEquals(MyState.NOT_LOANED, sm.dispatch(MyEvent.PressForceReturn))
        assertEquals(
            listOf(
                "action_PressForceReturn",
                "out_Lock",
                "out_OnLoan",
                "in_NotLoaned"
            ),
            history
        )
    }

    @Test
    fun test_ChildStateInheritsEdgesOfParent_WithIntoChildState() {
        val sm = stateMachine(MyState.LOCK)

        history.clear()
        assertEquals(MyState.EMERGENCY_LOCK, sm.dispatch(MyEvent.PressEmergencyLock))
        assertEquals(
            listOf(
                "action_PressEmergencyLock",
                "in_EmergencyLock"
            ),
            history
        )
    }

    @Test
    fun test_generatePlantUml() {
        val builder = smBuilder(MyState.NOT_LOANED)

        println(builder.generatePlantUml())
    }
}
