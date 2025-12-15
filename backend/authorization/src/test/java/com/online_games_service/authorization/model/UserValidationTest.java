package com.online_games_service.authorization.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

public class UserValidationTest {

    private Validator validator;

    @BeforeClass
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidUser() {
        // Given
        User user = new User("ValidNick", false);

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        Assert.assertTrue(violations.isEmpty(), "Valid user should not have errors");
    }

    @Test
    public void testUsernameIsBlank() {
        // Given
        User user = new User("", false); 
        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        Assert.assertFalse(violations.isEmpty());
        boolean hasBlankError = violations.stream()
                .anyMatch(v -> v.getMessage().contains("cannot be blank") || v.getMessage().contains("between"));
        Assert.assertTrue(hasBlankError);
    }

    @Test
    public void testUsernameIsTooShort() {
        // Given
        User user = new User("ab", false); 

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        Assert.assertEquals(violations.size(), 1);
        Assert.assertEquals(violations.iterator().next().getMessage(), "Username must be between 3 and 20 characters");
    }

    @Test
    public void testUsernameIsTooLong() {
        // Given
        String longNick = "a".repeat(21);
        User user = new User(longNick, false);

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        Assert.assertEquals(violations.size(), 1);
        Assert.assertEquals(violations.iterator().next().getMessage(), "Username must be between 3 and 20 characters");
    }
}