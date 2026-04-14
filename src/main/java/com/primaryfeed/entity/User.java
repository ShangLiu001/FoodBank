package com.primaryfeed.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // never return in JSON
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // 0=Staff, 1=Volunteer
    @Column(nullable = false)
    private Byte role;

    // 0=Inactive, 1=Active
    @Column(nullable = false)
    private Byte status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private FoodBankBranch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Address address;

    @Column(name = "driving_license_num")
    private String drivingLicenseNum;

    // Helper: Spring Security role string — @JsonIgnore so Jackson never calls it
    @JsonIgnore
    public String getRoleName() {
        if (role == null) return "ROLE_VOLUNTEER";
        return role == 0 ? "ROLE_STAFF" : "ROLE_VOLUNTEER";
    }
}