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
 * Channel through which a transaction is initiated, e.g. ATM or Mobile App.
 * <p>
 * Read-only: rows are created by Liquibase (or upstream systems) and never changed
 * by this service, so {@code @Immutable} stops Hibernate from ever updating them.
 */
@Entity
@Immutable
@Table(name = "Channel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // required by JPA, hidden from application code
@ToString
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChannelID")
    private Integer id;

    @Column(name = "ChannelName", nullable = false, unique = true, length = 40)
    private String name;
}
