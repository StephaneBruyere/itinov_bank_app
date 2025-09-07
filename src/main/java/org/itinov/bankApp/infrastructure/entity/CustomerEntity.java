package org.itinov.bankApp.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a Customer in the banking application.
 * Each customer can have multiple accounts.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String keycloakId;
    private String name;
    private String email;

//    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
//    @Builder.Default
//    private List<AccountEntity> accounts = new ArrayList<>();
}
