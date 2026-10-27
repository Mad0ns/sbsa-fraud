package za.co.sbsa.sbsa_fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import za.co.sbsa.sbsa_fraud.domain.converter.RiskLevelConverter;
import za.co.sbsa.sbsa_fraud.domain.converter.TransactionStatusConverter;

/**
 * A financial transaction submitted for processing and fraud screening.
 * <p>
 * A new transaction starts as {@link TransactionStatus#PENDING}. Screening then
 * moves it to {@link TransactionStatus#SUCCESS} or holds it as
 * {@link TransactionStatus#FLAGGED} (FR-07); it never changes again after that.
 */
@Entity
@Table(name = "`Transaction`")   // TRANSACTION is a reserved word in MySQL, so the name is quoted
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionID")
    private Integer id;

    /** Caller-supplied unique reference, e.g. SB100000; used to reject duplicates (FR-03). */
    @Column(name = "TransactionRef", nullable = false, unique = true, length = 20)
    private String transactionRef;

    // LAZY: related rows are only loaded if the code actually uses them.
    // ToString.Exclude: printing a transaction must not trigger extra database queries.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountID", nullable = false)
    @ToString.Exclude
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ChannelID", nullable = false)
    @ToString.Exclude
    private Channel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LocationID")
    @ToString.Exclude
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TransactionTypeID")
    @ToString.Exclude
    private TransactionType transactionType;

    /** Amount in ZAR. BigDecimal, never double: binary floating point cannot hold cents exactly. */
    @Column(name = "Amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Calendar date (UTC) of the transaction, kept for WM 02 reporting compatibility. */
    @Column(name = "TransactionDate", nullable = false)
    private LocalDate transactionDate;

    /** Moment the channel recorded the transaction, stored in UTC (Q6). Drives the velocity window. */
    @Column(name = "TransactionTimestamp", nullable = false)
    private Instant transactionTimestamp;

    @Convert(converter = TransactionStatusConverter.class)
    @Column(name = "Status", nullable = false, length = 20)
    private TransactionStatus status;

    /** Supplied risk rating from the WM 02 data; not set for transactions processed by this service. */
    @Convert(converter = RiskLevelConverter.class)
    @Column(name = "RiskLevel", length = 20)
    private RiskLevel riskLevel;

    /**
     * Creates a newly received transaction in {@link TransactionStatus#PENDING} state.
     *
     * @param transactionRef       unique reference supplied by the channel
     * @param account              existing account the transaction is posted to
     * @param channel              channel that initiated it
     * @param location             city, or {@code null} if unknown
     * @param transactionType      kind of transaction, or {@code null} if unknown
     * @param amount               amount in ZAR, greater than zero
     * @param transactionTimestamp when the channel recorded it
     */
    public Transaction(String transactionRef, Account account, Channel channel, Location location,
                       TransactionType transactionType, BigDecimal amount, Instant transactionTimestamp) {
        this.transactionRef = transactionRef;
        this.account = account;
        this.channel = channel;
        this.location = location;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionTimestamp = transactionTimestamp;
        this.transactionDate = LocalDate.ofInstant(transactionTimestamp, ZoneOffset.UTC);
        this.status = TransactionStatus.PENDING;
    }

    /** Marks the transaction as cleared by fraud screening. */
    public void approve() {
        requirePending();
        this.status = TransactionStatus.SUCCESS;
    }

    /** Holds the transaction because at least one fraud rule fired (FR-07). */
    public void flag() {
        requirePending();
        this.status = TransactionStatus.FLAGGED;
    }

    private void requirePending() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                    "Transaction " + transactionRef + " has already been decided: " + status);
        }
    }
}
