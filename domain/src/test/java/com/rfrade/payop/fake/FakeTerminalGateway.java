package com.rfrade.payop.fake;

import com.rfrade.payop.domain.model.AuthResult;
import com.rfrade.payop.domain.model.OperationResult;
import com.rfrade.payop.domain.repository.TerminalRepository;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Single;

public class FakeTerminalGateway implements TerminalRepository {

    private final Map<UUID, Queue<AuthResult>> authorizeResponses = new ConcurrentHashMap<>();
    private final Map<UUID, Queue<OperationResult>> captureResponses = new ConcurrentHashMap<>();
    private final Map<UUID, Queue<OperationResult>> cancelResponses = new ConcurrentHashMap<>();

    private final Map<UUID, AtomicInteger> authorizeCounts = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> captureCounts = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> cancelCounts = new ConcurrentHashMap<>();

    private AuthResult defaultAuthorizeResponse;
    private OperationResult defaultCaptureResponse;
    private OperationResult defaultCancelResponse;

    public void setDefaultAuthorizeResponse(AuthResult result) {
        this.defaultAuthorizeResponse = result;
    }

    public void setDefaultCaptureResponse(OperationResult result) {
        this.defaultCaptureResponse = result;
    }

    public void setDefaultCancelResponse(OperationResult result) {
        this.defaultCancelResponse = result;
    }

    public int totalAuthorizeCallCount() {
        int total = 0;
        for (AtomicInteger count : authorizeCounts.values()) {
            total += count.get();
        }
        return total;
    }

    @Override
    public Single<AuthResult> authorize(UUID txnId, long amountCents) {
        authorizeCounts.computeIfAbsent(txnId, k -> new AtomicInteger(0)).incrementAndGet();
        return Single.fromCallable(
                () -> dequeue(authorizeResponses, txnId, defaultAuthorizeResponse));
    }

    @Override
    public Single<OperationResult> capture(UUID txnId, long amountCents) {
        captureCounts.computeIfAbsent(txnId, k -> new AtomicInteger(0)).incrementAndGet();
        return Single.fromCallable(() -> dequeue(captureResponses, txnId, defaultCaptureResponse));
    }

    @Override
    public Single<OperationResult> cancel(UUID txnId) {
        cancelCounts.computeIfAbsent(txnId, k -> new AtomicInteger(0)).incrementAndGet();
        return Single.fromCallable(() -> dequeue(cancelResponses, txnId, defaultCancelResponse));
    }

    private <T> T dequeue(Map<UUID, Queue<T>> map, UUID txnId, T defaultResponse) {
        Queue<T> queue = map.get(txnId);
        if (queue != null && !queue.isEmpty()) {
            return queue.poll();
        }
        if (defaultResponse != null) {
            return defaultResponse;
        }
        throw new IllegalStateException("No response configured for txnId=" + txnId);
    }
}
