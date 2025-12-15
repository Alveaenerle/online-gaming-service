package com.online_games_service.authorization.model;

import com.online_games_service.authorization.model.Account;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

public class AccountValidationTest {

    private Validator validator;

    @BeforeClass
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidAccount() {
        // Given
        String userId = "123"; 
        String username = "validUser";
        Account account = new Account("test@example.com", "generated_hash_string_xyz", userId, username);

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertTrue(violations.isEmpty(), "Valid account should not have validation errors");
    }

    @Test
    public void testInvalidEmail() {
        // Given
        String userId = "123";
        String username = "validUser";
        Account account = new Account("nie-to-nie-jest-email", "someHash123", userId, username);

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertEquals(violations.size(), 1, "Should have exactly 1 violation");
        ConstraintViolation<Account> violation = violations.iterator().next();
        Assert.assertEquals(violation.getMessage(), "Invalid email format");
    }

    @Test
    public void testPasswordHashIsBlank() {
        // Given
        String userId = "123";
        String username = "validUser";
        Account account = new Account("valid@email.com", "", userId, username); 

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertFalse(violations.isEmpty(), "Blank password hash should trigger error");
    }

    @Test
    public void testIdIsNullBeforeSave() {
        // Given
        String userId = "123";
        String username = "validUser";
        Account account = new Account("test@test.com", "passHash123", userId, username);
        
        // When
        String id = account.getId();
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertNull(id, "ID should be null before saving to DB");
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void testUserIdIsBlank() {        
        // Given
        String username = "validUser";
        Account account = new Account("valid@email.com", "validPass", "", username); 

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertFalse(violations.isEmpty(), "Account with blank userId should be invalid");
        
        boolean hasUserIdError = violations.stream()
                .anyMatch(v -> v.getMessage().contains("User ID cannot be blank"));
        Assert.assertTrue(hasUserIdError, "Missing validation error for blank userId");
    }

    @Test
    public void testUserIdIsNull() {
        // Given
        String username = "validUser";
        Account account = new Account("valid@email.com", "validPass", null, username);

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertFalse(violations.isEmpty(), "Account with null userId should be invalid");
    }

    @Test
    public void testUsernameIsBlank() {
        // Given
        String userId = "123";
        Account account = new Account("valid@email.com", "validPass", userId, ""); 

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertFalse(violations.isEmpty(), "Account with blank username should be invalid");
        
        boolean hasError = violations.stream()
                .anyMatch(v -> v.getMessage().contains("Username cannot be blank"));
        Assert.assertTrue(hasError, "Missing validation error for blank username");
    }

    @Test
    public void testUsernameIsTooShort() {
        // Given
        String userId = "123";
        Account account = new Account("valid@email.com", "validPass", userId, "ab");

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertFalse(violations.isEmpty(), "Account with short username should be invalid");
        
        boolean hasError = violations.stream()
                .anyMatch(v -> v.getMessage().contains("Username must be between 3 and 20 characters"));
        Assert.assertTrue(hasError, "Missing validation error for short username");
    }

    @Test
    public void testUsernameIsTooLong() {
        // Given
        String userId = "123";
        String longUsername = "a".repeat(21);
        Account account = new Account("valid@email.com", "validPass", userId, longUsername);

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertFalse(violations.isEmpty(), "Account with long username should be invalid");
        
        boolean hasError = violations.stream()
                .anyMatch(v -> v.getMessage().contains("Username must be between 3 and 20 characters"));
        Assert.assertTrue(hasError, "Missing validation error for long username");
    }
}