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
        Account account = new Account("test@example.com", "generated_hash_string_xyz");

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertTrue(violations.isEmpty(), "Valid account should not have validation errors");
    }

    @Test
    public void testInvalidEmail() {
        // Given
        Account account = new Account("nie-to-nie-jest-email", "someHash123");

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
        Account account = new Account("valid@email.com", ""); // Pusty hash

        // When
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertFalse(violations.isEmpty(), "Blank password hash should trigger error");
    }

    @Test
    public void testIdIsNullBeforeSave() {
        // Given
        Account account = new Account("test@test.com", "passHash123");
        
        // When
        String id = account.getId();
        Set<ConstraintViolation<Account>> violations = validator.validate(account);

        // Then
        Assert.assertNull(id, "ID should be null before saving to DB");
        Assert.assertTrue(violations.isEmpty());
    }
}