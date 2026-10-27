package za.co.sbsa.sbsa_fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Immutable;

/**
 * Bank account that transactions are posted against. Opened upstream; this service only checks that it exists (FR-02).
 * <p>
 * Read-only: rows are created by Liquibase (or upstream systems) and never changed
 * by this service, so {@code @Immutable} stops Hibernate from ever updating them.
 */
@Entity
@Immutable
@Table(name = "Account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // required by JPA, hidden from application code
@ToString
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AccountID")
    private Integer id;

    @Column(name = "AccountNumber", nullable = false, unique = true, length = 20)
    private String accountNumber;
}
