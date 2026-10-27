package za.co.sbsa.sbsa_fraud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import za.co.sbsa.sbsa_fraud.domain.TestData;
import za.co.sbsa.sbsa_fraud.domain.Transaction;
import za.co.sbsa.sbsa_fraud.domain.TransactionStatus;
import za.co.sbsa.sbsa_fraud.repository.AccountRepository;
import za.co.sbsa.sbsa_fraud.repository.ChannelRepository;
import za.co.sbsa.sbsa_fraud.repository.LocationRepository;
import za.co.sbsa.sbsa_fraud.repository.TransactionTypeRepository;
import za.co.sbsa.sbsa_fraud.service.exception.InvalidTransactionException;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionRequest;

/** Unit tests for business validation against the database and the clock (FR-02). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)   // each test only reaches some lookups
class TransactionValidatorTest {

    private static final Instant NOW = Instant.parse("2026-10-27T09:00:00Z");

    @Mock private AccountRepository accountRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private TransactionTypeRepository transactionTypeRepository;
    @Mock private LocationRepository locationRepository;

    private TransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator(accountRepository, channelRepository,
                transactionTypeRepository, locationRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        when(accountRepository.findByAccountNumber(anyString())).thenReturn(Optional.empty());
        when(accountRepository.findByAccountNumber("4128728463"))
                .thenReturn(Optional.of(TestData.account(1, "4128728463")));
        when(channelRepository.findByName("ATM")).thenReturn(Optional.of(TestData.channel("ATM")));
        when(transactionTypeRepository.findByTypeName("POS Purchase"))
                .thenReturn(Optional.of(TestData.transactionType("POS Purchase")));
        when(locationRepository.findByCity("Durban")).thenReturn(Optional.of(TestData.location("Durban")));
    }

    private static TransactionRequest request(String account, String channel, String type, String city,
                                              Instant timestamp) {
        return new TransactionRequest("T-1", account, new BigDecimal("50.00"), channel, type, city, timestamp);
    }

    private static TransactionRequest valid() {
        return request("4128728463", "ATM", "POS Purchase", "Durban", NOW.minusSeconds(60));
    }

    @Test
    @DisplayName("Valid request becomes a PENDING transaction with its UTC date")
    void validRequestBuildsPendingTransaction() {
        Transaction transaction = validator.toValidTransaction(valid());

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(transaction.getAccount().getAccountNumber()).isEqualTo("4128728463");
        assertThat(transaction.getLocation().getCity()).isEqualTo("Durban");
        assertThat(transaction.getTransactionDate()).isEqualTo(LocalDate.of(2026, 10, 27));
    }

    @Test
    @DisplayName("TC-02: unknown account is rejected with INVALID_ACCOUNT")
    void unknownAccount() {
        assertThatThrownBy(() -> validator.toValidTransaction(
                request("9999999999", "ATM", "POS Purchase", null, NOW)))
                .isInstanceOf(InvalidTransactionException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_ACCOUNT");
    }

    @Test
    @DisplayName("TC-04: unknown channel is rejected with INVALID_CHANNEL")
    void unknownChannel() {
        assertThatThrownBy(() -> validator.toValidTransaction(
                request("4128728463", "Fax", "POS Purchase", null, NOW)))
                .hasFieldOrPropertyWithValue("code", "INVALID_CHANNEL");
    }

    @Test
    @DisplayName("TC-04: unknown transaction type is rejected with INVALID_TRANSACTION_TYPE")
    void unknownType() {
        assertThatThrownBy(() -> validator.toValidTransaction(
                request("4128728463", "ATM", "Barter", null, NOW)))
                .hasFieldOrPropertyWithValue("code", "INVALID_TRANSACTION_TYPE");
    }

    @Test
    @DisplayName("Unknown city is rejected; no city at all is allowed")
    void cityIsOptionalButMustExistIfGiven() {
        assertThatThrownBy(() -> validator.toValidTransaction(
                request("4128728463", "ATM", "POS Purchase", "Atlantis", NOW)))
                .hasFieldOrPropertyWithValue("code", "INVALID_CITY");

        assertThat(validator.toValidTransaction(request("4128728463", "ATM", "POS Purchase", null, NOW))
                .getLocation()).isNull();
    }

    @Test
    @DisplayName("TC-05 / Q6: timestamp more than 5 minutes in the future is rejected")
    void futureTimestampRejected() {
        assertThatThrownBy(() -> validator.toValidTransaction(
                request("4128728463", "ATM", "POS Purchase", null, NOW.plusSeconds(301))))
                .hasFieldOrPropertyWithValue("code", "INVALID_TIMESTAMP");
    }

    @Test
    @DisplayName("Q6 boundary: exactly 5 minutes ahead is still accepted")
    void fiveMinutesAheadAccepted() {
        assertThat(validator.toValidTransaction(
                request("4128728463", "ATM", "POS Purchase", null, NOW.plusSeconds(300)))).isNotNull();
    }
}
