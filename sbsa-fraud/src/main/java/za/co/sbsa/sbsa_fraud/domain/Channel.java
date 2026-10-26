package za.co.sbsa.sbsa_fraud.domain;


import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/**
 * Channel through which a transaction is initiated, e.g. ATM or Mobile App.
 * Lookup data: rows are created by Liquibase and never changed by the application.
 */


@Immutable
@Entity
@Table(name = "Channel")
public class Channel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ChannelID")
    private Integer id;

    @Column(name = "ChannelName", nullable = false, unique = true)
    private String name;


    protected Channel() {
    }

    ;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
