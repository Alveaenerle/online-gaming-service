package com.online_games_service.authorization.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Password cannot be blank")
    private String passwordHash;

    @NotBlank(message = "User ID cannot be blank")
    @Indexed(unique = true)
    private String userId;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @CreatedDate
    private LocalDateTime createdAt;

    public Account(String email, String passwordHash, String userId, String username) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.userId = userId;
        this.username = username;
    }
}
