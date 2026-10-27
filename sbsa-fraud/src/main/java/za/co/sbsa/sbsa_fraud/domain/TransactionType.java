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
 * Kind of transaction, e.g. EFT Payment or ATM Withdrawal.
 * <p>
 * Read-only: rows are created by Liquibase (or upstream systems) and never changed
 * by this service, so {@code @Immutable} stops Hibernate from ever updating them.
 */
@Entity
@Immutable
@Table(name = "TransactionType")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // required by JPA, hidden from application code
@ToString
public class TransactionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionTypeID")
    private Integer id;

    @Column(name = "TypeName", nullable = false, unique = true, length = 40)
    private String typeName;
}
