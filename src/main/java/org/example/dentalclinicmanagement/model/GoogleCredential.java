// NEW  src/main/java/com/secure/notes/models/GoogleCredential.java
package org.example.dentalclinicmanagement.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity @Data
@Table(name = "google_credentials")
public class GoogleCredential {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)        // користувач, якому належить токен
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(length = 4000) private String refreshToken;
    @Column(length = 4000) private String accessToken;
    private Instant accessTokenExpiry;       // google повертає seconds-to-expiry
}
