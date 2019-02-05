# statek

FSM DSL in kotlin.

![Maven Version](https://img.shields.io/github/tag/tomoya0x00/statek.svg?style=flat&label=maven)

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

for more details, please see [FsmTest](https://github.com/tomoya0x00/statek/blob/master/src/commonTest/kotlin/FsmTest.kt)

### Usage with gradle:

```
def statekVersion = "0.3.0"

repositories {
    maven { url "https://dl.bintray.com/tomoya0x00/maven" }
}

// For multiplatform projects
implementation "com.github.tomoya0x00:statek:$statekVersion"

// For jvm projects
implementation "com.github.tomoya0x00:statek-jvm:$statekVersion"
```
