# Payment Terminal — SDK Reference

Reference card for what each `PaymentTerminal` method can return. Keep this next to your code while integrating.

The API surface is in the AAR's `README.md`; this document is the result codes only, the rules for when each fires, and how to interpret them.

---

## 1. `authorize(txnId, amountCents)` → `LiveData<AuthorizationResult>`

| `state`     | `error`                  | `approvedAmount`     | Meaning                                                                 |
|-------------|--------------------------|----------------------|-------------------------------------------------------------------------|
| `APPROVED`  | `null`                   | ≤ requested, ≥ 1     | Terminal authorised. Held funds are `approvedAmount`. May be less than requested. |
| `DECLINED`  | `INSUFFICIENT_FUNDS`     | `0`                  | Processor refused for funding reasons.                                  |
| `DECLINED`  | `INVALID_AMOUNT`         | `0`                  | `amountCents ≤ 0`. Client-side validation echoed.                       |
| `DECLINED`  | `DECLINED`               | `0`                  | Generic refusal — processor said no without a specific reason.          |
| `TIMED_OUT` | `NETWORK_ERROR`          | `0`                  | The SDK did not hear back from the processor. **You do not know whether the transaction landed.** |

**Critical:** A `TIMED_OUT` authorisation must be resolved by issuing `cancel(txnId)`, not by retrying `authorize`. Retrying `authorize` with the same UUID *will charge twice* if the original landed. The cancel path interrogates the terminal: it returns `APPROVED` (it landed, now voided) or `UNKNOWN_TRANSACTION_ID` (it never landed). Either way you have a definite answer.

---

## 2. `capture(txnId, amountCents)` → `LiveData<TerminalResult>`

| `state`     | `error`                       | Meaning                                                              |
|-------------|-------------------------------|----------------------------------------------------------------------|
| `APPROVED`  | `null`                        | Capture committed for `amountCents`.                                 |
| `DECLINED`  | `OVER_AUTHORIZED_AMOUNT`      | `amountCents` exceeds the previously approved amount.                |
| `DECLINED`  | `INVALID_AMOUNT`              | `amountCents ≤ 0`.                                                   |
| `DECLINED`  | `ALREADY_CAPTURED`            | This `txnId` was already captured. **Idempotent success for retries** — money already moved. |
| `DECLINED`  | `ALREADY_CANCELLED`           | This `txnId` was already cancelled. Capture is not possible.         |
| `DECLINED`  | `UNKNOWN_TRANSACTION_ID`      | The terminal has no record of this `txnId`. The authorisation was lost — capture is not possible. |
| `TIMED_OUT` | `NETWORK_ERROR`               | Outcome unknown. Retry the same call until you get a definite state. |

**Retry semantics:** Capture is safe to retry on `TIMED_OUT` because the worst outcome on a retry is `ALREADY_CAPTURED` — which means a previous attempt did land. `ALREADY_CAPTURED` should be treated by your code as success.

---

## 3. `cancel(txnId)` → `LiveData<TerminalResult>`

| `state`     | `error`                       | Meaning                                                              |
|-------------|-------------------------------|----------------------------------------------------------------------|
| `APPROVED`  | `null`                        | Authorisation voided. Held funds released.                           |
| `DECLINED`  | `ALREADY_CAPTURED`            | This `txnId` was already captured. Cancel is not possible — money already moved. |
| `DECLINED`  | `ALREADY_CANCELLED`           | This `txnId` was already cancelled. **Idempotent success for retries.** |
| `DECLINED`  | `UNKNOWN_TRANSACTION_ID`      | The terminal has no record of this `txnId`. Treat as success: the auth never landed, so there is nothing to void. |
| `TIMED_OUT` | `NETWORK_ERROR`               | Outcome unknown. Retry until a definite state is returned.           |

**Retry semantics:** Cancel is safe to retry on `TIMED_OUT`. Both `APPROVED` and `UNKNOWN_TRANSACTION_ID` resolve to "the transaction is cancelled"; `ALREADY_CANCELLED` confirms a prior attempt landed.

---

## 4. Distribution by Mode

| Outcome distribution per call | `HAPPY` | `REAL` | `POOR` |
|--------------------------------|--------:|-------:|-------:|
| `TIMED_OUT` (NETWORK_ERROR)    |      0 % |    10 % |    35 % |
| `DECLINED` (random)            |      0 % |    10 % |    35 % |
| Partial approval (authorise)   |      0 % |     5 % |    15 % |

Per-call latency:

| Outcome                                   | Wall-clock delay  |
|-------------------------------------------|-------------------|
| `APPROVED`, `DECLINED` (any error code)   | random 5–10 s     |
| `TIMED_OUT` (`NETWORK_ERROR`)             | exactly 10 s      |
| Synchronous validation (`INVALID_AMOUNT`) | ~100 ms           |

The long normal latency exists on purpose: it gives transitioning states (`AUTHORIZING`, `CAPTURING`, `CANCELLING`) enough wall-clock time to be visible in the UI.

Note that *deterministic* declines (the rule-driven ones — `OVER_AUTHORIZED_AMOUNT`, `ALREADY_CAPTURED`, `UNKNOWN_TRANSACTION_ID`, etc.) happen at 100 % rate whenever the rule triggers, in all modes. The percentages above are only for the random network failures and random refusals that happen on otherwise-valid calls.

---

## 5. Threading

- Each method returns its `LiveData` synchronously, immediately. The work happens on the SDK's internal single-thread executor.
- `LiveData` emissions happen on the Android **main thread**, per AndroidX convention. If your subscriber does heavy work, hop off the main thread.
- The SDK serialises calls — there is exactly one worker thread. Submit several calls and they queue.

---

## 6. Process lifecycle

- The SDK's transaction memory is **in-process only**. A fresh `PaymentTerminal.create(...)` after your process dies has no knowledge of prior `txnId`s.
- Any post-restart `capture(uuid, _)` or `cancel(uuid)` for a `uuid` issued before the restart will return `UNKNOWN_TRANSACTION_ID`. That is **not** an error — it is evidence that the SDK does not remember the transaction. Your code interprets it per call type (see tables 2 and 3).
