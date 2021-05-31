# statek

![Maven Version](https://img.shields.io/github/tag/tomoya0x00/statek.svg?style=flat&label=maven)

Multiplatform (JVM, JS, iOS Kotlin/Native) Kotlin DSL for FSM (finite state machine)

## Some samples:

```kotlin
stateMachine(initial = MyState.NOT_LOANED) {
   state(MyState.NOT_LOANED) {
       edge<MyEvent.PressRental>(MyState.LOCK)
   }
   state(MyState.ON_LOAN, entry = { /* LED ON */ }, exit = { /* LED OFF */ }) {
       state(MyState.LOCK) {
           edge<MyEvent.PressUnLock>(MyState.UNLOCK)
           edge<MyEvent.PressLock>(MyState.NOT_LOANED, guard = { it.isLongPress })
       }
       state(MyState.UNLOCK) {
           edge<MyEvent.PressLock>(MyState.LOCK, guard = { !it.isLongPress })
       }
   }
}
```

for more details, please see [FsmTest](https://github.com/tomoya0x00/statek/blob/master/statek/src/commonTest/kotlin/FsmTest.kt)

### Usage with gradle:

```
repositories {
    mavenCentral()
}

def statekVersion = "0.5.0"

// For multiplatform projects
implementation "io.github.tomoya0x00:statek:$statekVersion"

// For jvm projects
implementation "io.github.tomoya0x00:statek-jvm:$statekVersion"
```
