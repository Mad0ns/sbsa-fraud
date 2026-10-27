package za.co.sbsa.sbsa_fraud.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Builds entities for unit tests.
 * <p>
 * The lookup entities have protected constructors and no setters, by design. This class
 * lives in the same package as the entities, so it can use those constructors, and sets
 * fields with {@link ReflectionTestUtils}, so production code stays read-only.
 */
public final class TestData {

    public static final String VELOCITY = "VELOCITY_MICRO";

    private TestData() {
    }

    public static Account account(int id, String accountNumber) {
        Account account = new Account();
        ReflectionTestUtils.setField(account, "id", id);
        ReflectionTestUtils.setField(account, "accountNumber", accountNumber);
        return account;
    }

    public static Channel channel(String name) {
        Channel channel = new Channel();
        ReflectionTestUtils.setField(channel, "id", 1);
        ReflectionTestUtils.setField(channel, "name", name);
        return channel;
    }

    public static Location location(String city) {
        Location location = new Location();
        ReflectionTestUtils.setField(location, "id", 1);
        ReflectionTestUtils.setField(location, "city", city);
        return location;
    }

    public static TransactionType transactionType(String typeName) {
        TransactionType type = new TransactionType();
        ReflectionTestUtils.setField(type, "id", 1);
        ReflectionTestUtils.setField(type, "typeName", typeName);
        return type;
    }

    /** The velocity rule with the agreed thresholds: 5 x R100.00 or less in 10 minutes. */
    public static FraudRule velocityRule() {
        return rule(VELOCITY, 5, new BigDecimal("100.00"), 10);
    }

    public static FraudRule rule(String code, Integer maxCount, BigDecimal ceiling, Integer windowMinutes) {
        FraudRule rule = new FraudRule();
        ReflectionTestUtils.setField(rule, "id", 1);
        ReflectionTestUtils.setField(rule, "ruleCode", code);
        ReflectionTestUtils.setField(rule, "description", "test rule " + code);
        ReflectionTestUtils.setField(rule, "maxCount", maxCount);
        ReflectionTestUtils.setField(rule, "amountCeiling", ceiling);
        ReflectionTestUtils.setField(rule, "windowMinutes", windowMinutes);
        ReflectionTestUtils.setField(rule, "active", true);
        return rule;
    }

    /** A new, unsaved PENDING transaction on the given account. */
    public static Transaction transaction(Account account, String amount, Instant timestamp) {
        return new Transaction(uniqueRef(), account, channel("ATM"), null,
                transactionType("POS Purchase"), new BigDecimal(amount), timestamp);
    }

    /** A reference that fits the 20-character column and never repeats. */
    public static String uniqueRef() {
        return "T-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
