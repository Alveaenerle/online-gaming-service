package com.online_games_service.authorization.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document(collection = "accounts")
@Data
public class Account {

    @Id
    private String id;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    @Indexed(unique = true)
    private String email;

    @NotBlank
    private String passwordHash;

    public Account(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }
}