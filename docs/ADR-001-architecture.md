# ADR-001: Persistence-Ordering and Recovery

## Context
The payment terminal SDK (AAR) is non-idempotent and stateless across process death. A crash after an `authorize(uuid)` call but before recording the result can lead to double charges if the app simply retries on restart.

Constraints:
- SDK is asynchronous (LiveData/Main thread).
- SDK is serial (single worker thread).
- Must avoid double charges at all costs.
- Unknown outcomes must be resolved via `cancel(txnId)`.

## Decision
Room journaling with persist-before-call ordering via RxJava.

Pattern: `persist(state) -> SDK call -> persist(result)`

Recovery logic:
- On launch, scan for non-terminal states.
- If `AUTHORIZING` + timeout/crash: Do not re-authorize. Update to `CANCELLING` and issue `cancel(txnId)`.
- The `cancel` call voids the authorization or resolves `UNKNOWN_TRANSACTION_ID`. Both resolve to a terminal state.

Implementation details:
- Room `@Transaction` used for 50-entry FIFO limit during upserts.
- RxJava `Completable` and `Single` to sequence DB writes before SDK calls.
- `Schedulers.io()` for DB operations to ensure flush before next step.

## Alternatives Considered
- **SharedPreferences**: Lacks transaction support and FIFO management. `apply()` is insufficient for strict ordering.
- **In-memory + Async flush**: Introduces crash window where journal lags SDK call, violating safety requirements.
- **Manual SQLite/File Log**: Feasible, but Room provides built-in Rx support and compile-time SQL validation.

## Impact
- **Safety**: Database acts as source of truth for pre-invocation intent, mitigating double charges.
- **Performance**: Adds ~2-5ms overhead per call for DB writes, acceptable given 5-10s SDK response times.
- **Testing**: Deterministic recovery tested by pre-seeding DB and running recovery use case against fake SDK.
