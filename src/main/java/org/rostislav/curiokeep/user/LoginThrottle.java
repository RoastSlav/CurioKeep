package org.rostislav.curiokeep.user;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows password guessing by counting failed logins in a sliding window, per client address and per account from that address.
 * <p>
 * The account limit is scoped to the address on purpose: a limit on the account alone would let anyone lock the real user out
 * by failing logins for their email. State is in memory, which matches the single-node deployment and resets on restart.
 */
@Component
public class LoginThrottle {

    public static final int MAX_FAILURES_PER_ADDRESS = 20;
    public static final int MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TRACKED_KEYS = 10_000;

    private record Attempts(int failures, Instant windowStart) {
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginThrottle() {
        this(Clock.systemUTC());
    }

    LoginThrottle(Clock clock) {
        this.clock = clock;
    }

    /** How long the caller has to wait before another attempt is accepted, or empty when it may try now. */
    public Optional<Duration> blockedFor(String address, String email) {
        Instant now = clock.instant();
        Optional<Duration> byAddress = remaining(addressKey(address), MAX_FAILURES_PER_ADDRESS, now);
        Optional<Duration> byAccount = remaining(accountKey(address, email), MAX_FAILURES_PER_ACCOUNT_FROM_ADDRESS, now);
        return byAddress.isPresent() && byAccount.isPresent()
                ? Optional.of(byAddress.get().compareTo(byAccount.get()) >= 0 ? byAddress.get() : byAccount.get())
                : byAddress.or(() -> byAccount);
    }

    public void recordFailure(String address, String email) {
        Instant now = clock.instant();
        if (attempts.size() >= MAX_TRACKED_KEYS) {
            attempts.values().removeIf(a -> expired(a, now));
            if (attempts.size() >= MAX_TRACKED_KEYS) attempts.clear(); // bounded memory beats perfect bookkeeping under a flood
        }
        for (String key : new String[]{addressKey(address), accountKey(address, email)}) {
            attempts.merge(key, new Attempts(1, now),
                    (old, fresh) -> expired(old, now) ? fresh : new Attempts(old.failures() + 1, old.windowStart()));
        }
    }

    public void recordSuccess(String address, String email) {
        attempts.remove(accountKey(address, email));
    }

    private Optional<Duration> remaining(String key, int limit, Instant now) {
        Attempts a = attempts.get(key);
        if (a == null || expired(a, now)) return Optional.empty();
        return a.failures() >= limit ? Optional.of(Duration.between(now, a.windowStart().plus(WINDOW))) : Optional.empty();
    }

    private static boolean expired(Attempts a, Instant now) {
        return !now.isBefore(a.windowStart().plus(WINDOW));
    }

    private static String addressKey(String address) {
        return "addr|" + address;
    }

    private static String accountKey(String address, String email) {
        return "acct|" + address + "|" + email;
    }
}
