package com.spring.JavaT.customer;

import com.spring.JavaT.common.BaseEntity;
import com.spring.JavaT.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * A utility customer registered with WASAC/REG.
 *
 * <p>May optionally be linked to a {@link User} account with {@link com.spring.JavaT.user.Role#CUSTOMER}
 * for self-service bill viewing.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "full_names", nullable = false, length = 100)
    private String fullNames;

    @Column(name = "national_id", nullable = false, unique = true, length = 16)
    private String nationalId;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    /** Returns the linked user id, or null when no portal account exists. */
    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }
}
