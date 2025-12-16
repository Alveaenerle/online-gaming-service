package com.online_games_service.authorization.model;

import com.online_games_service.authorization.dto.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

public class RegisterRequestValidationTest {

    private Validator validator;

    @BeforeClass
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidRegisterRequest() {
        // Given
        RegisterRequest request = new RegisterRequest("ValidNick", "valid@email.com", "password123");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        Assert.assertTrue(violations.isEmpty(), "Valid request should not have errors");
    }

    @Test
    public void testUsernameIsBlank() {
        // Given
        RegisterRequest request = new RegisterRequest("", "valid@email.com", "password123");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        Assert.assertFalse(violations.isEmpty());
        boolean hasUsernameError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        Assert.assertTrue(hasUsernameError, "Should have username validation error");
    }

    @Test
    public void testUsernameIsTooShort() {
        // Given
        RegisterRequest request = new RegisterRequest("ab", "valid@email.com", "password123");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        boolean hasSizeError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username") 
                            && v.getMessage().contains("between"));
        Assert.assertTrue(hasSizeError, "Should verify username length");
    }

    @Test
    public void testUsernameIsTooLong() {
        // Given
        String longNick = "a".repeat(21);
        RegisterRequest request = new RegisterRequest(longNick, "valid@email.com", "password123");

        // When
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        // Then
        boolean hasSizeError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username"));
        Assert.assertTrue(hasSizeError, "Should verify username length");
    }
}