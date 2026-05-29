# PayOp — Payment Operator App

An Android app that integrates a payment-terminal SDK. Drives transactions via a state machine, logs state transitions to disk prior to SDK invocation, and handles process death recovery to prevent double charges.

## Architecture

- **Data**: Room persistence, repository implementations, terminal SDK encapsulation.
- **Domain**: Pure Java. Use cases, repository interfaces, state machine, and RxBus command processor for asynchronous transaction routing.
- **Presentation**: MVI pattern. ViewModels provided via Dagger Multibindings and `ViewModelProvider.Factory`.

Key feature: State transitions are persisted prior to SDK invocation to enable process death recovery.

## Build, Install, Run

```bash
# Build
./gradlew clean :app:assembleDebug

# Run tests
./gradlew :app:testDebugUnitTest

# Install on connected device/emulator
./gradlew :app:installDebug
```

Requirements: JDK 17/21 (for AGP 8.1.4), Android SDK with compileSdk 33.

## Libraries & Rationale

| Library | Version | Why |
|---------|---------|-----|
| **Room** | 2.5.2 | Structured persistence with compile-time SQL validation, Rx integration (Flowable observation), and `@Transaction` for atomic upsert+FIFO trim. |
| **RxJava 2** | 2.2.21 | Async composition for the persist→call→persist chain. `Single` for SDK calls, `Flowable` for Room observation, `Completable` for persistence. The brief specifies RxJava 2. |
| **RxAndroid** | 2.1.1 | `AndroidSchedulers.mainThread()` for UI observation. |
| **Dagger 2** | 2.48.1 | Compile-time DI. I split the modules (`AppModule`, `DataModule`, `DomainModule`, `TerminalModule`) across the subprojects to keep boundaries rigid. |
| **Material Components** | 1.9.0 | Material Design widgets (FAB, MaterialButton, TextInputLayout). |
| **Gson** | 2.10.1 | JSON serialization for terminal data and persistence models. |
| **JUnit 4** | 4.13.2 | JVM-only unit tests as specified in the brief. |

## Future Improvements & Trade-offs

- **Main Thread Testing**: Some Rx bridges involving `LiveData` rely on the main thread `Looper`, making them hard to unit test without Robolectric. I used fakes for the domain logic tests, but adding instrumented tests would cover these edge cases.
- **Persistent Backoff**: The retry count is persisted, but the exponential backoff delay is reset if the process dies mid-wait. Persisting a "next retry" timestamp would fix this.
- **UI/UX**: Relied on Material defaults per the brief. Could be improved with formatted currency, state-specific badges, and a "terminal busy" indicator when the SDK's serial worker thread is occupied.

## AI Usage

I used AI to write the boilerplate XML layouts for the UI. Since styling isn't important, this saved me some time so I could focus entirely on the state machine, architecture, and concurrency requirements.
