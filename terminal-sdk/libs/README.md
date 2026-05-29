# Payment Terminal — Android Library (AAR)

An Android library that simulates a payment terminal **in-process**. Shipped as an AAR you depend on — the same shape a real payment-terminal SDK takes.

## What's in the box

The public API is exactly one interface and a handful of supporting types:

```java
public interface PaymentTerminal {

    /** Construct an instance. Non-deterministic seed. */
    static PaymentTerminal create(Mode mode);

    /** Construct an instance with a deterministic seed. Two terminals with the
     *  same (mode, seed) produce identical outcomes modulo thread-scheduling
     *  jitter. Use this in tests. */
    static PaymentTerminal create(Mode mode, long seed);

    LiveData<AuthorizationResult> authorize(UUID txnId, long amountCents);
    LiveData<TerminalResult>      capture  (UUID txnId, long amountCents);
    LiveData<TerminalResult>      cancel   (UUID txnId);

    enum Mode { HAPPY, REAL, POOR }
}
```

Plus the result/error types under the same package:

```java
public final class AuthorizationResult {
    public final State        state;            // APPROVED | DECLINED | TIMED_OUT
    @Nullable public final ErrorCode error;     // null iff state == APPROVED
    public final long         approvedAmount;   // ≤ requested; 0 if not APPROVED
}

public final class TerminalResult {
    public final State        state;
    @Nullable public final ErrorCode error;
}

public enum State { APPROVED, DECLINED, TIMED_OUT }

public enum ErrorCode {
    NETWORK_ERROR,           // always paired with TIMED_OUT
    INSUFFICIENT_FUNDS,
    INVALID_AMOUNT,
    OVER_AUTHORIZED_AMOUNT,
    ALREADY_CAPTURED,
    ALREADY_CANCELLED,
    UNKNOWN_TRANSACTION_ID,
    DECLINED                 // generic decline from the processor
}
```

The implementation behind `PaymentTerminal` is private and obfuscated by ProGuard. The instance returned by `PaymentTerminal.create(...)` is the only handle you need. Do not try to read the bytecode — this README and the SDK reference are the contract.

## How to consume it

Drop the pre-built `.aar` into your project's `app/libs/` folder, then:

```gradle
// app/build.gradle
dependencies {
    implementation files('libs/payment-terminal.aar')

    // Recommended: the AndroidX bridge between LiveData and reactive streams,
    // so your Rx pipeline can consume the SDK's LiveData without leaking
    // lifecycle owners. Use however you like.
    implementation 'androidx.lifecycle:lifecycle-reactivestreams:2.6.2'
    implementation 'io.reactivex.rxjava2:rxjava:2.2.21'
}
```

In production this dependency would be a real payment-terminal SDK. The interface contract is the same; only the wiring changes.

## Behaviour you need to know

### The SDK is not idempotent
Calling `authorize(uuid, 100)` twice with the same UUID lands two charges on the terminal. There is no built-in deduplication. Your code must prevent duplicates by construction. The simulator faithfully reproduces this in every mode.

### LiveData semantics
- Each call returns a fresh `LiveData` instance. It will emit the final result exactly once, then stay at that value (a `LiveData` always holds its last value).
- Emissions happen on the **main thread**, per Android convention. If you do heavy work in your observer, hop off the main thread first.
- The SDK keeps no internal reference to the `LiveData` you don't observe — it just goes away. That said, the underlying call still runs to completion; if you call `authorize` and never observe the result, the charge still lands on the terminal.

### The terminal channel is serial
Internally the SDK has one worker thread. Submit two `authorize` calls and the second is queued behind the first. This mirrors the physical reality of one terminal. Your scheduler must not assume parallel calls.

### No persistence inside the SDK
The SDK's memory of which `txnId`s it has seen lives in instance state. **It does not survive your process death.** A fresh `PaymentTerminal.create(...)` after a restart has no record of any prior transaction. A `capture` or `cancel` call for a `txnId` the SDK has not seen returns `DECLINED` with `errorCode = UNKNOWN_TRANSACTION_ID`. This is the load-bearing fact for your recovery logic.

### Mode

| Mode    | Network errors | Random declines | Partial approval | Use it for                                  |
|---------|---------------:|----------------:|-----------------:|---------------------------------------------|
| `HAPPY` |             0 % |              0 % |               0 % | Development sanity checks; deterministic tests |
| `REAL`  |            10 % |             10 % |               5 % | What we grade against                       |
| `POOR`  |            35 % |             35 % |              15 % | Demo recording; stress tests                |

Per-call latency:
- **Normal responses** (APPROVED, DECLINED) — random in **5–10 s**.
- **`TIMED_OUT`** — exactly **10 s** (the timeout boundary).
- **Synchronous validation failures** (`INVALID_AMOUNT`) — ~100 ms (no processor round-trip).

The long normal latency exists on purpose: it gives transitioning states (`AUTHORIZING`, `CAPTURING`, `CANCELLING`) enough wall-clock time to be visible in the UI.

### Validation behaviour
- `authorize(_, amount ≤ 0)` → `DECLINED` + `INVALID_AMOUNT`.
- `capture(uuid, amount > approvedAmount)` → `DECLINED` + `OVER_AUTHORIZED_AMOUNT`.
- `capture(uuid, amount ≤ 0)` → `DECLINED` + `INVALID_AMOUNT`.
- `capture(uuid, _)` on a transaction already captured → `DECLINED` + `ALREADY_CAPTURED`.
- `capture(uuid, _)` on a transaction already cancelled → `DECLINED` + `ALREADY_CANCELLED`.
- `cancel(uuid)` on a transaction already captured → `DECLINED` + `ALREADY_CAPTURED`.
- `cancel(uuid)` on a transaction already cancelled → `DECLINED` + `ALREADY_CANCELLED`.
- Any call for an unknown `txnId` → `DECLINED` + `UNKNOWN_TRANSACTION_ID`.

## Building the AAR (graders / authors only)

```bash
./gradlew :payment-terminal:assembleRelease     # produces build/outputs/aar/payment-terminal-release.aar (ProGuard'd)
./gradlew :payment-terminal:candidateBundle     # packages the AAR + README + SDK reference as build/distributions/payment-terminal-*.zip
```
