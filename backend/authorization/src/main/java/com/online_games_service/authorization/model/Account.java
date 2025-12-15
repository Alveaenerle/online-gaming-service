package com.online_games_service.authorization.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "accounts")
@Data
@NoArgsConstructor
public class Account {

    @Id
    private String id;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    @Indexed(unique = true)
    private String email;

    @NotBlank
    private String passwordHash;

    @Indexed(unique = true)
    private String userId;

    @CreatedDate
    private LocalDateTime createdAt;

    public Account(String email, String passwordHash, String userId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.userId = userId;
    }
}
