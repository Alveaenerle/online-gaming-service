package com.online_games_service.authorization.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

public class GoogleOAuthRequestTest {

    private Validator validator;

    @BeforeClass
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void shouldCreateValidRequest() {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("valid-id-token");

        // When
        Set<ConstraintViolation<GoogleOAuthRequest>> violations = validator.validate(request);

        // Then
        Assert.assertTrue(violations.isEmpty());
        Assert.assertEquals(request.getIdToken(), "valid-id-token");
    }

    @Test
    public void shouldFailValidationWhenIdTokenIsNull() {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest(null);

        // When
        Set<ConstraintViolation<GoogleOAuthRequest>> violations = validator.validate(request);

        // Then
        Assert.assertFalse(violations.isEmpty());
        Assert.assertTrue(violations.stream()
            .anyMatch(v -> v.getMessage().contains("required")));
    }

    @Test
    public void shouldFailValidationWhenIdTokenIsEmpty() {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("");

        // When
        Set<ConstraintViolation<GoogleOAuthRequest>> violations = validator.validate(request);

        // Then
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldFailValidationWhenIdTokenIsBlank() {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("   ");

        // When
        Set<ConstraintViolation<GoogleOAuthRequest>> violations = validator.validate(request);

        // Then
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldCreateWithNoArgsConstructor() {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest();

        // When
        request.setIdToken("set-token");

        // Then
        Assert.assertEquals(request.getIdToken(), "set-token");
    }

    @Test
    public void shouldSupportEquals() {
        // Given
        GoogleOAuthRequest request1 = new GoogleOAuthRequest("token");
        GoogleOAuthRequest request2 = new GoogleOAuthRequest("token");
        GoogleOAuthRequest request3 = new GoogleOAuthRequest("different");

        // Then
        Assert.assertEquals(request1, request2);
        Assert.assertNotEquals(request1, request3);
    }

    @Test
    public void shouldSupportHashCode() {
        // Given
        GoogleOAuthRequest request1 = new GoogleOAuthRequest("token");
        GoogleOAuthRequest request2 = new GoogleOAuthRequest("token");

        // Then
        Assert.assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    public void shouldSupportToString() {
        // Given
        GoogleOAuthRequest request = new GoogleOAuthRequest("test-token");

        // When
        String toString = request.toString();

        // Then
        Assert.assertTrue(toString.contains("test-token"));
    }
}
