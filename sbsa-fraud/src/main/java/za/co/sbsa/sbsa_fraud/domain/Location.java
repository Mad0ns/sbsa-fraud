package za.co.sbsa.sbsa_fraud.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

/**
 * Location in which the transaction was made.
 * Lookup data: rows are created by Liquibase and never changed by the application.
 */

@Immutable
@Table(name = "Location")
@Entity
public class Location {

    @Id
    @Column(name = "LocationID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    protected Location() {
    };

    @Column(name = "City", nullable = false, unique = true, length = 30)
    private String city;

    public Integer getLocationId() {
        return id;
    }

    public String getCity() {
        return city;
    }
}
