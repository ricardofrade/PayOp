# Senior Android — Stage 1 Offline Challenge

> **Take-home portion of the Elecctro senior Android process.**
> Senior-grade scope — designed to look like the code you'd actually ship at Elecctro on day one, in the corner of the codebase that touches real money. Imperfect-but-honest beats polished-but-shallow: a clear "if I had more time" section in your README is worth more than half-finished features you couldn't quite get right. Read the spec carefully and ask clarifying questions when something is ambiguous — good questions are a positive signal, and guessing the spec when you could have asked is the failure mode we're trying to avoid. AI use is fine — we use it too — just disclose what you used and where, because Stage 2 is a live walkthrough where you'll be asked to explain your own code.

---

## 1. The challenge — *"The Payment Operator App"*

Build an Android app that consumes a **payment-terminal SDK** (AAR, provided by us), lets a finance operator drive transactions through a real payment-terminal-shaped state machine, and persists a journal of every transition so that a process kill never costs the customer a double charge.

The SDK exposes a single public type — `PaymentTerminal` — with three async methods: `authorize`, `capture`, `cancel`. It returns `LiveData`. It is intentionally non-idempotent. It occasionally times out. It processes one call at a time. And it forgets everything when your process dies. You hold the truth on disk, drive the state machine, and resume in-flight work on launch.

This mirrors, in shape, a real Elecctro embedded-payments integration: an Android app fronting a callback-based terminal SDK we don't control, journalling state to disk, and surfacing it to a human operator. We picked the scenario because it exercises what we care about at senior level — async stack literacy, persistence-ordering discipline, recovery design, and the discipline to refuse to guess in code that touches money.

---

## 2. What you need to build

### Hard requirements (must-have)

1. Add `payment-terminal.aar` to your project (`app/libs/` + `implementation files('libs/payment-terminal.aar')`). See the `README.md` shipped alongside the AAR (inside the same zip).
2. **Authorise.** A form (amount in cents, plus whatever else makes your UI work — the wire takes only `(UUID, long)`). Submitting it adds the transaction to the history immediately in state `AUTHORIZING`, generates a fresh UUID, calls `terminal.authorize`, and resolves to a definite state.
3. **Capture, full or partial.** For any transaction in `AUTHORIZED`, the operator can capture for any amount in `[1, approvedAmount]`. Over-capture is rejected client-side without a transport call; the wire will also reject with `OVER_AUTHORIZED_AMOUNT` if asked.
4. **Cancel.** For any transaction in `AUTHORIZED` (void) or `AUTHORIZING` (interrupt), the operator can cancel. Cancelling a transaction already in a terminal state is rejected at the UI; do not issue a transport call.
5. **Resolve `TIMED_OUT` authorisations.** A `TIMED_OUT` from `authorize` means the SDK did not receive a response from the processor — we do not know whether the customer was charged. The system must drive that transaction into `CANCELLING` automatically and retry until the terminal returns a definite outcome. If the original authorisation landed, the cancel will succeed; if it never landed, the cancel will return `UNKNOWN_TRANSACTION_ID`. Either is a valid resolution; do **not** assume cancelled without confirmation from the terminal.
6. **Retry strategy.** `CAPTURING` and `CANCELLING` retry on `TIMED_OUT` with exponential backoff and a configurable maximum. After the max, the transaction lands in `CAPTURE_FAILED` / `CANCEL_FAILED` with the last-seen error code surfaced, and an operator-driven "retry" action becomes available. We do not grade specific backoff numbers; we grade that you have a strategy and that it terminates.
7. **Multi-transaction concurrency.** Several transactions can sit in `AUTHORIZED`, `CAPTURING`, `CANCELLING`, … at the same time. Starting a new authorisation while another is parked at `AUTHORIZED` is fine and expected. The terminal serialises calls internally; your scheduler must not assume otherwise.
8. **No duplicate charges.** A process kill, retry, or UI restart must never cause a second `authorize` to land on the terminal for the same logical transaction. Where the outcome of a previous call is genuinely unknown, the system must drive the transaction to a definite state by **interrogating the terminal** (via `cancel` / `capture` retries that observe `UNKNOWN_TRANSACTION_ID` or `APPROVED`), never by guessing.
9. **Recovery on launch.** After a process kill, the app must restore the history and resume in-flight work:
   - Transactions journalled as `AUTHORIZING` → resume by calling `cancel(txnId)` (their fate is unknown; the only safe move is to interrogate). The SDK will return `UNKNOWN_TRANSACTION_ID` if the original authorise never landed (resolve to `CANCELLED`), or `APPROVED` if it did and is now voided (resolve to `CANCELLED`).
   - Transactions journalled as `CAPTURING` → resume by retrying `capture(txnId, amount)`. The SDK will return `UNKNOWN_TRANSACTION_ID` (the authorise was lost — resolve to `CAPTURE_FAILED`) or `ALREADY_CAPTURED` (we captured before the crash and didn't journal it — resolve to `CAPTURED`) or eventually `APPROVED`.
   - Transactions journalled as `CANCELLING` → resume by retrying `cancel(txnId)`. Same evidence rules.
   - Transactions in terminal states are restored to history and not touched.
10. **Persistence-before-call ordering.** Every state transition is journalled to disk **before** the SDK call that advances it is issued, and **before** any UI subscriber is notified. This is the property that makes recovery safe — reverse it and the no-double-charge guarantee silently breaks.
11. **History view & bounded storage.** Persist at most **50 transactions** — the most recent by creation time. Inserting a 51st must drop the oldest (FIFO). The scrollable history shows those 50, newest first. Each row shows at least: `txnId` (abbreviated), amount, current state (including retry count where applicable, e.g. `CAPTURING (#3)`), last-updated timestamp. Tap to drill into a detail view: full state-transition log, plus action buttons (Capture, Cancel, Retry) where the state allows. The cap is a real storage cap, not just a display cap — the device's persistent state stays bounded regardless of how many transactions an operator runs over the device's lifetime.
12. **Tests.** Automated, JVM-only. Must cover at minimum:
    - The per-transaction state machine: every legal transition; every illegal transition rejected.
    - The `TIMED_OUT` authorisation → `CANCELLING` chain.
    - Recovery: at least one test per recoverable state (`AUTHORIZING`, `CAPTURING`, `CANCELLING`), pre-seeding the journal and re-instantiating the app's state holder against a fresh `PaymentTerminal` (no in-process memory of the prior txn — exactly what happens at real startup).
    - No-double-charge: an authorisation that times out and then resolves via the cancel chain must result in **at most one** call to `terminal.authorize(txnId, …)`. The SDK provides no built-in test affordance for this — designing a test that proves the property (a counting decorator over your `PaymentTerminal` dependency, for example) is part of what we evaluate.
13. **Architecture Decision Record.** In `docs/ADR-001-architecture.md`, write a one-page ADR (≈ 500 words) covering **one** load-bearing architectural choice you made. Suggested topics: per-transaction state modelling, the `LiveData → Rx` bridge and its lifecycle implications, the retry scheduler's process-death survival, or your persistence-ordering / recovery design. Structure it as: **Context** / **Decision** / **Alternatives considered** (at least two, with one-sentence trade-off each) / **Consequences** (what becomes easy, what becomes harder, what you'd re-evaluate at scale).

### Stretch (nice but not required)

- Inject the `PaymentTerminal` instance behind an interface you own, so a fake/stub could replace it in tests without touching your domain code.
- Surface retry counts and last-seen error codes in the UI's detail view; they're useful in the demo recording.
- A connection-status indicator that surfaces SDK health (e.g. last `NETWORK_ERROR` timestamp).

### Out of scope

- Networking — the terminal AAR is in-process; no TCP, no HTTP.
- Cryptography.
- Beautiful UI / animations — we do not grade aesthetics. Material defaults are fine.
- Multi-user / authentication.

---

## 3. The per-transaction state machine

Each transaction has its own lifecycle. The app holds many of these at any time, in any combination of states.

```
                    authorize(txnId, amount)
                              │
                              ▼
                       ┌─────────────┐
                       │ AUTHORIZING │── DECLINED ──→ DECLINED
                       └──────┬──────┘── TIMED_OUT ──┐
                              │ APPROVED              │
                              ▼                       │
                       ┌─────────────┐                │
                       │ AUTHORIZED  │                │
                       │  (parked)   │                │
                       └──────┬──────┘                │
                              │                       │
              capture(txnId,amount)    cancel(txnId)  │
                              │           │           │
                              ▼           ▼           │
                       ┌─────────────┐ ┌─────────────┐│
                       │ CAPTURING   │ │ CANCELLING  │◀──── (auto-cancel
                       │  (retry #N) │ │  (retry #N) │      the timed-out
                       └──────┬──────┘ └──────┬──────┘      authorise)
                              │               │
            APPROVED / ALREADY_CAPTURED  APPROVED /
                              │       UNKNOWN_TRANSACTION_ID
                              ▼               ▼
                       ┌─────────────┐ ┌─────────────┐
                       │  CAPTURED   │ │  CANCELLED  │
                       └─────────────┘ └─────────────┘

                  Any retried call may also resolve to DECLINED
                  (then the transaction lands in CAPTURE_FAILED /
                  CANCEL_FAILED depending on which call failed).
```

**One fused state machine per transaction.** A transaction goes through `AUTHORIZING → AUTHORIZED → CAPTURING → CAPTURED` (the happy path) or branches into `CANCELLING → CANCELLED`. A `TIMED_OUT` authorisation drives directly into `CANCELLING` (more on that above). Do not model authorisation, capture, and cancellation as three independent aggregates that share a `txnId` — that invites referential-integrity bugs.

Terminal states (per transaction): `CAPTURED`, `CANCELLED`, `DECLINED`, `CAPTURE_FAILED`, `CANCEL_FAILED`. Once terminal, the transaction does not change again.

User-facing labels are yours to choose. The exact names above are illustrative; pick what reads well in your UI.

---

## 4. The library API

```java
import com.elecctro.recruitment.paymentterminal.PaymentTerminal;

public interface PaymentTerminal {

    /** In-process instance. Remembers transactions it has seen until your
     *  process dies — at which point a fresh instance has no memory of anything. */
    static PaymentTerminal create(Mode mode);
    static PaymentTerminal create(Mode mode, long seed);

    LiveData<AuthorizationResult> authorize(UUID txnId, long amountCents);
    LiveData<TerminalResult>      capture  (UUID txnId, long amountCents);
    LiveData<TerminalResult>      cancel   (UUID txnId);

    enum Mode { HAPPY, REAL, POOR }
}

class AuthorizationResult {
    State        state;            // APPROVED | DECLINED | TIMED_OUT
    @Nullable ErrorCode error;     // null iff APPROVED
    long         approvedAmount;   // ≤ requestedAmount; 0 if not APPROVED
}

class TerminalResult {
    State        state;            // APPROVED | DECLINED | TIMED_OUT
    @Nullable ErrorCode error;     // null iff APPROVED
}

enum State     { APPROVED, DECLINED, TIMED_OUT }
enum ErrorCode {
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

Things you need to know about how this SDK behaves — and that the spec is silent on at the API surface:

- **The methods are not idempotent.** Calling `authorize(uuid, 100)` twice with the same UUID charges twice. Avoid duplicates by construction.
- **Returns `LiveData`.** The SDK exposes a lifecycle-aware async type. Your project's stack is RxJava 2. Bridging the two cleanly is part of what we evaluate.
- **The terminal channel is serial.** Internally the SDK processes one call at a time. You can submit several; they queue. Do not assume parallelism.
- **No persistence inside the SDK.** When your process dies the SDK forgets everything. A fresh `PaymentTerminal` on app launch has no record of any prior `txnId`. The disk journal is your only memory.
- **`Mode` controls failure rates:**
  - `HAPPY` — no network errors, no random declines, no partial approval. For development sanity checks.
  - `REAL` — 10 % network errors, 10 % random declines, 5 % partial approval. The mode we grade against.
  - `POOR` — 35 % network errors, 35 % random declines, 15 % partial approval. Useful for forcing the timeout / retry / recovery scenes in your demo recording.

Full mode reference, call-by-call semantics, and the complete result-code table are in the `README.md` and `payment-terminal-sdk-reference.md` that ship alongside the AAR.

---

## 5. What you deliver

A GitHub repo (public or private — invite us if private) containing:

1. The Android Studio project (`gradlew`, `app/`, etc.) **with `payment-terminal.aar` checked in under `app/libs/`**. `./gradlew :app:assembleDebug` must succeed on a clean clone.
2. A **README.md** with:
   - One paragraph describing the shape of your architecture.
   - How to build, install, and run.
   - Libraries you chose and why (one line each).
   - **Known issues / "if I had more time"** section — be honest, this section is read carefully.
   - A line about any AI assistance you used and where.
3. The **ADR** at `docs/ADR-001-architecture.md` (see §2 for the structure we expect). We're looking for an honest comparison with concrete trade-offs, not a one-line justification.
4. A short **screen recording (5–8 minutes — Loom, or any tool)** of the app. Pick whichever `Mode` makes each scene easy to demonstrate — `REAL` for happy-path flows, `POOR` for the timeout/retry/recovery flows. Show:
   1. **New authorisation — happy path** — fill the form → see the row appear in `AUTHORIZING` → state advances to `AUTHORIZED`.
   2. **Capture full amount** — open the row → capture for the authorised amount → state goes to `CAPTURED`.
   3. **Capture partial amount** — different transaction → capture less than authorised → state goes to `CAPTURED`.
   4. **Partial approval** — a transaction where the terminal approved less than requested; the UI surfaces the delta.
   5. **Decline** — a transaction the terminal declined; show the decline reason.
   6. **`TIMED_OUT` authorisation resolved via cancel chain** — the row goes `AUTHORIZING → CANCELLING (#1) → CANCELLING (#2) → CANCELLED` without a duplicate authorisation hitting the terminal.
   7. **Cancel during AUTHORIZED** — authorise → wait → cancel → `CANCELLED`.
   8. **Capture retry hitting `UNKNOWN_TRANSACTION_ID`** — capture retries after a timeout until the terminal admits it doesn't have the txn; transaction lands in `CAPTURE_FAILED` with the reason visible.
   9. **Multi-transaction concurrency** — start a second authorisation while a first is mid-`CAPTURING`; both rows update independently.
   10. **Kill + restart recovery** — get an `AUTHORIZING` row, swipe the app from Recents, reopen → the row is resumed via the cancel chain, no double charge, history restored.
5. Tests runnable via `./gradlew :app:testDebug` (or wherever you place them), covering at minimum the suites listed in §2.12.

Send the repo URL and the recording link to **talent@elecctro.com**.

---

## 6. Time & ground rules

- **Calendar window:** 7 days from when you receive this brief. We're not measuring speed; we're measuring what you deliver. If something genuinely blocks you (illness, family commitments), tell us and we'll extend. That said, scoping and bounding your own work is part of what we're looking at — use the time you have rather than asking for more without a strong reason.
- **Language: Java 8.** Non-negotiable. Most of our production Android code is Java; we want to see you in the language we'd pair with you on. Kotlin in tests is OK.
- **Build target:** `compileSdk 33`, `minSdk 23`, `targetSdk 33`. Tests should be JVM-only (no instrumentation). Emulator is fine for the recording.
- **AI policy:** Use Claude, Copilot, ChatGPT — whatever helps. Disclose what you used and where in the README. We don't penalise AI use itself — what we mark down is the work it produces when used carelessly: boilerplate that doesn't fit the brief, ignored edge cases, untested generated code that double-charges on the first timeout. Stage 2 is a live walkthrough, so be ready to talk through anything in your repo.
- **Don't over-build.** UI polish isn't graded; if you're choosing between polishing the UI and writing one more recovery test, choose the test. A complete, clean, smaller solution beats an ambitious half-finished one — we grade what's in front of us, not what you intended to do.
- **Scope down before you stretch.** If a feature is taking longer than you'd defend in a senior code review, cut it back, document the gap in your README's "if I had more time" section, and move on. We grade an honest "I scoped X out because of Y" higher than we grade a sprawling half-finished attempt.

---

## 7. How we grade you

| Criterion                                                | Weight |
|----------------------------------------------------------|--------|
| Recovery & no-double-charge (the load-bearing property)  | 20 %   |
| Architecture & module boundaries                         | 15 %   |
| Per-transaction state machine correctness                | 15 %   |
| Async stack quality (LiveData↔Rx adapter, scheduling)    | 15 %   |
| Multi-transaction concurrency & retry scheduler          | 10 %   |
| Testing strategy                                         | 10 %   |
| Java 8 quality                                           |  5 %   |
| ADR / written reasoning                                  |  5 %   |
| README & demo recording                                  |  5 %   |

Each criterion is scored 0–5 and weighted. Your total comes out of 100. We pass at **70**, with mandatory minimums: Recovery & no-double-charge ≥ 4, Async stack ≥ 3, Architecture ≥ 3, no criterion < 3. Two engineers grade independently.

### Anti-patterns we'll be looking for

Knowing them up front is half the battle.

- **Retrying `authorize` on `TIMED_OUT` instead of issuing `cancel`.** The single most common bug in payments code. If a retry of `authorize` lands on the same UUID after the original also landed, you've double-charged. The cancel chain exists exactly to avoid this — use it.
- **Assuming cancelled without confirmation from the terminal.** "We timed out, so we'll just mark it cancelled" loses money in production. Every cancellation must be confirmed by the terminal (either `APPROVED` or `UNKNOWN_TRANSACTION_ID`).
- **State persisted *after* the SDK call, or *after* the UI subscriber is notified.** Silently breaks the no-double-charge guarantee under a crash.
- **`try { … } catch (Exception e) { /* log */ }` swallowing inside the state machine or the retry loop.** Swallowed errors hide exactly the failure modes the recovery code most needs to react to.
- **Unbounded retries with no escape hatch.** Pins CPU and battery; never surfaces the failure to the operator.
- **A monolithic Activity that handles everything** — no boundary between UI, domain, and the SDK.
- **`observeForever` on a `LiveData` returned by the SDK, never removed.** Memory leak — every `observeForever` needs a matching `removeObserver`.
- **The SDK's `LiveData` observed only on an Activity's `LifecycleOwner`.** Retries running in the background lose their observer the moment the Activity is destroyed. The bridge is one of the things we explicitly evaluate.

### Things that make us happy

- A test that demonstrably catches a real recovery bug if we mutate one line of your code.
- An ADR that says "I would have done X if I had the budget" and explains why.
- A `LiveData → Flowable` (or equivalent) bridge that handles disposal and process-lifecycle observation cleanly.
- A retry scheduler that resumes correctly after a process kill mid-backoff.
- A clean SDK seam — a fake `PaymentTerminal` you could drop in for unit tests without changing your domain code.

---

## 8. What's in the zip we sent you

The attachment is a single zip — `senior-android-challenge-<version>.zip` (e.g. `senior-android-challenge-1.0.0.zip`) — containing this brief and a nested zip with the SDK:

```
senior-android-challenge-1.0.0.zip
├── challenge-brief.md                ← this document
└── payment-terminal-1.0.0.zip
    ├── payment-terminal.aar          ← the SDK — drop into your app/libs/
    ├── README.md                     ← how to consume the AAR (modes, threading, lifecycle notes)
    └── payment-terminal-sdk-reference.md  ← result codes and detailed behaviour
```

Unzip anywhere. You only check `payment-terminal.aar` into your repo (`app/libs/`); the README and SDK reference are for use while you code. Do not try to read the AAR's bytecode — the SDK is ProGuard-obfuscated on purpose.

---

## 9. Questions?

For clarifying questions, or to deliver your submission, write to **Elecctro Talent — talent@elecctro.com**. We aim to respond within one business day. Clarifying questions about the spec are welcome; we won't debug your code for you.

Good luck — we look forward to seeing what you build.
