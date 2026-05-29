package com.rfrade.payop.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.rfrade.payop.domain.model.RetryPolicy;

import org.junit.Test;

public class RetryPolicyTest {

    @Test
    public void delay_for_first_attempt_is_base_delay() {
        RetryPolicy policy = new RetryPolicy(5, 1000, 2.0, 30000);
        assertEquals(1000, policy.delayForAttempt(0));
    }

    @Test
    public void delay_doubles_each_attempt() {
        RetryPolicy policy = new RetryPolicy(5, 1000, 2.0, 30000);
        assertEquals(1000, policy.delayForAttempt(0));
        assertEquals(2000, policy.delayForAttempt(1));
        assertEquals(4000, policy.delayForAttempt(2));
    }

    @Test
    public void delay_capped_at_max() {
        RetryPolicy policy = new RetryPolicy(10, 1000, 2.0, 10000);
        assertEquals(10000, policy.delayForAttempt(5));
    }

    @Test
    public void defaults_are_sensible() {
        RetryPolicy policy = new RetryPolicy();
        assertEquals(5, policy.getMaxRetries());
        assertTrue(policy.getBaseDelayMs() > 0);
    }
}
