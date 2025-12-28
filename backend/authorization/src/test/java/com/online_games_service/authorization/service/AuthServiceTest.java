package com.online_games_service.authorization.service;

import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.exception.EmailAlreadyExistsException;
import com.online_games_service.authorization.exception.InvalidCredentialsException;
import com.online_games_service.authorization.model.Account;
import com.online_games_service.authorization.model.User;
import com.online_games_service.authorization.repository.AccountRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        authService = new AuthService(accountRepository, passwordEncoder);
    }

    // REGISTER TESTS

    @Test
    public void shouldRegisterNewAccountSuccessfully() {
        // Given
        RegisterRequest request = new RegisterRequest("testuser", "test@test.com", "password123");
        String encodedPass = "encoded_password_hash";
        
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn(encodedPass);

        // When
        authService.register(request);

        // Then
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());

        Account savedAccount = accountCaptor.getValue();
        Assert.assertEquals(savedAccount.getEmail(), request.getEmail());
        Assert.assertEquals(savedAccount.getUsername(), request.getUsername());
        Assert.assertEquals(savedAccount.getPasswordHash(), encodedPass);
        
        Assert.assertNotNull(savedAccount.getUserId());
        Assert.assertFalse(savedAccount.getUserId().isEmpty());
    }

    @Test(expectedExceptions = EmailAlreadyExistsException.class)
    public void shouldThrowExceptionWhenRegisteringExistingEmail() {
        // Given
        RegisterRequest request = new RegisterRequest("user", "existing@test.com", "pass");
        
        when(accountRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When
        authService.register(request);

        // Then
        verify(accountRepository, never()).save(any()); 
    }

    @Test
    public void shouldHandleExceptionDuringRegistration() {
        // Given
        RegisterRequest request = new RegisterRequest("user", "test@test.com", "pass");
        when(accountRepository.existsByEmail(any())).thenThrow(new RuntimeException("DB Error"));

        // When
        try {
            authService.register(request);
            Assert.fail("Should have thrown exception");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(), "DB Error");
        }
    }

    // LOGIN TESTS

    @Test
    public void shouldLoginSuccessfullyAndReturnUserDTO() {
        // Given
        LoginRequest request = new LoginRequest("test@test.com", "password123");
        String encodedPass = "encoded_hash";
        String existingUserId = "user-uuid-123";
        
        Account foundAccount = new Account("test@test.com", encodedPass, existingUserId, "testuser");

        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(foundAccount));
        when(passwordEncoder.matches(request.getPassword(), encodedPass)).thenReturn(true);

        // When
        User result = authService.login(request);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), existingUserId);
        Assert.assertEquals(result.getUsername(), "testuser");
        Assert.assertFalse(result.isGuest());
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenAccountNotFound() {
        // Given
        LoginRequest request = new LoginRequest("missing@test.com", "pass");
        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // When
        authService.login(request);
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenPasswordIsIncorrect() {
        // Given
        LoginRequest request = new LoginRequest("test@test.com", "wrong_pass");
        Account foundAccount = new Account("test@test.com", "correct_hash", "id", "user");

        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(foundAccount));
        when(passwordEncoder.matches(request.getPassword(), "correct_hash")).thenReturn(false);

        // When
        authService.login(request);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void shouldPropagateExceptionWhenRepositoryFailsDuringLogin() {
        // Given
        LoginRequest request = new LoginRequest("test@test.com", "pass");
        when(accountRepository.findByEmail(any())).thenThrow(new RuntimeException("DB Error"));

        // When
        authService.login(request);
    }

    // GUEST TESTS

    @Test
    public void shouldGenerateValidGuestUser() { 
        // When
        User guest = authService.createGuest();

        // Then
        Assert.assertNotNull(guest);
        Assert.assertNotNull(guest.getId());
        Assert.assertTrue(guest.getUsername().startsWith("Guest_"));
        Assert.assertTrue(guest.isGuest());

        verifyNoInteractions(accountRepository);
    }
}