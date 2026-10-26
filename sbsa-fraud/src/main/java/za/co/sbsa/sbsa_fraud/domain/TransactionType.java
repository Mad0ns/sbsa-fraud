package za.co.sbsa.sbsa_fraud.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/**
 * Type of transaction made
 * Lookup data: rows are created by Liquibase and never changed by the application.
 */
@Entity
@Table(name = "Transactiontype")
@Immutable
public class TransactionType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransactionTypeID")
    private Integer id;

    @Column(name = "TypeName", nullable = false, unique = true)
    private String typeName;

    protected TransactionType() {
    }

    public Integer getTransactionTypeID() {
        return id;
    }

    public String getTypeName() {
        return typeName;
    }
}
