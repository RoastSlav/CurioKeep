package org.rostislav.curiokeep.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginThrottleTest {

    private static final String IP = "203.0.113.7";
    private static final String OTHER_IP = "203.0.113.8";

    private MutableClock clock;
    private LoginThrottle throttle;

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        throttle = new LoginThrottle(clock);
    }

    private void fail(String ip, String email, int times) {
        for (int i = 0; i < times; i++) throttle.recordFailure(ip, email);
    }

    @Test
    void allowsAttemptsUntilTheAccountLimitFromOneAddressIsReached() {
        fail(IP, "a@x.test", LoginThrottle.MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS - 1);
        assertThat(throttle.blockedFor(IP, "a@x.test")).isEmpty();

        throttle.recordFailure(IP, "a@x.test");

        assertThat(throttle.blockedFor(IP, "a@x.test")).isPresent();
    }

    @Test
    void failuresForOneAccountDoNotLockOutAnotherAccountOrAnotherAddress() {
        fail(IP, "victim@x.test", LoginThrottle.MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS);

        assertThat(throttle.blockedFor(IP, "victim@x.test")).isPresent();
        assertThat(throttle.blockedFor(OTHER_IP, "victim@x.test")).isEmpty();
        assertThat(throttle.blockedFor(IP, "someone-else@x.test")).isEmpty();
    }

    @Test
    void anAddressTryingManyAccountsIsBlockedOverall() {
        for (int i = 0; i < LoginThrottle.MAX_FAILURES_PER_ADDRESS; i++) {
            throttle.recordFailure(IP, "user" + i + "@x.test");
        }

        assertThat(throttle.blockedFor(IP, "brand-new@x.test")).isPresent();
        assertThat(throttle.blockedFor(OTHER_IP, "brand-new@x.test")).isEmpty();
    }

    @Test
    void reportsHowLongToWaitAndForgetsAfterTheWindow() {
        fail(IP, "a@x.test", LoginThrottle.MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS);
        clock.advance(Duration.ofMinutes(5));

        assertThat(throttle.blockedFor(IP, "a@x.test")).hasValue(Duration.ofMinutes(10));

        clock.advance(Duration.ofMinutes(10));

        assertThat(throttle.blockedFor(IP, "a@x.test")).isEmpty();
    }

    @Test
    void aSuccessfulLoginClearsTheAccountCounter() {
        fail(IP, "a@x.test", LoginThrottle.MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS - 1);

        throttle.recordSuccess(IP, "a@x.test");
        fail(IP, "a@x.test", LoginThrottle.MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS - 1);

        assertThat(throttle.blockedFor(IP, "a@x.test")).isEmpty();
    }
}
