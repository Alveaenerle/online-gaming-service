package com.online_games_service.authorization.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

public class LoginRequestValidationTest {

    private Validator validator;

    @BeforeClass
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testValidLoginRequest() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void testInvalidEmail() {
        LoginRequest request = new LoginRequest("invalid-email", "password");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        Assert.assertFalse(violations.isEmpty());
        Assert.assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    public void testBlankEmail() {
        LoginRequest request = new LoginRequest("", "password");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void testBlankPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        Assert.assertFalse(violations.isEmpty());
        Assert.assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }
}
