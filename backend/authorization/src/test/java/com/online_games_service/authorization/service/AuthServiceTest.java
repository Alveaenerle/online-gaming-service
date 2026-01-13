package com.online_games_service.authorization.service;

import com.online_games_service.authorization.dto.GoogleUserInfo;
import com.online_games_service.authorization.dto.LoginRequest;
import com.online_games_service.authorization.dto.RegisterRequest;
import com.online_games_service.authorization.dto.UpdateUsernameRequest;
import com.online_games_service.authorization.dto.UpdatePasswordRequest;
import com.online_games_service.authorization.exception.EmailAlreadyExistsException;
import com.online_games_service.authorization.exception.InvalidCredentialsException;
import com.online_games_service.authorization.exception.OAuthAccountException;
import com.online_games_service.authorization.exception.UsernameAlreadyExistsException;
import com.online_games_service.authorization.model.Account;
import com.online_games_service.authorization.model.AuthProvider;
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

    // UPDATE USERNAME TESTS

    @Test
    public void shouldUpdateUsernameSuccessfully() {
        // Given
        String userId = "user-123";
        String newUsername = "updatedUser";
        UpdateUsernameRequest request = new UpdateUsernameRequest(newUsername);
        Account account = new Account("test@test.com", "hash", userId, "oldUser");

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByUsername(newUsername)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        User result = authService.updateUsername(userId, request);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getUsername(), newUsername);
        Assert.assertEquals(result.getId(), userId);
        Assert.assertFalse(result.isGuest());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Assert.assertEquals(accountCaptor.getValue().getUsername(), newUsername);
    }

    @Test
    public void shouldAllowKeepingSameUsername() {
        // Given
        String userId = "user-123";
        String sameUsername = "existingUser";
        UpdateUsernameRequest request = new UpdateUsernameRequest(sameUsername);
        Account account = new Account("test@test.com", "hash", userId, sameUsername);

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When - should NOT throw even if existsByUsername would return true (because it's the same user)
        User result = authService.updateUsername(userId, request);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getUsername(), sameUsername);
        verify(accountRepository, never()).existsByUsername(sameUsername);
    }

    @Test(expectedExceptions = UsernameAlreadyExistsException.class)
    public void shouldThrowExceptionWhenUsernameAlreadyTaken() {
        // Given
        String userId = "user-123";
        String takenUsername = "takenUser";
        UpdateUsernameRequest request = new UpdateUsernameRequest(takenUsername);
        Account account = new Account("test@test.com", "hash", userId, "oldUser");

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(accountRepository.existsByUsername(takenUsername)).thenReturn(true);

        // When
        authService.updateUsername(userId, request);

        // Then - expects exception
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenUpdatingUsernameForNonExistentUser() {
        // Given
        String userId = "nonexistent";
        UpdateUsernameRequest request = new UpdateUsernameRequest("newName");

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        authService.updateUsername(userId, request);
    }

    @Test
    public void shouldHandleExceptionDuringUsernameUpdate() {
        // Given
        String userId = "user-123";
        UpdateUsernameRequest request = new UpdateUsernameRequest("newName");

        when(accountRepository.findByUserId(userId)).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        try {
            authService.updateUsername(userId, request);
            Assert.fail("Should have thrown exception");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(), "DB Error");
        }
    }

    // UPDATE PASSWORD TESTS

    @Test
    public void shouldUpdatePasswordSuccessfully() {
        // Given
        String userId = "user-123";
        String currentPassword = "oldPass123";
        String newPassword = "newPass456";
        String currentHash = "current_hash";
        String newHash = "new_hash";

        UpdatePasswordRequest request = new UpdatePasswordRequest(currentPassword, newPassword);
        Account account = new Account("test@test.com", currentHash, userId, "user");

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches(currentPassword, currentHash)).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(newHash);
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // When
        authService.updatePassword(userId, request);

        // Then
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Assert.assertEquals(accountCaptor.getValue().getPasswordHash(), newHash);
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenCurrentPasswordIsIncorrect() {
        // Given
        String userId = "user-123";
        UpdatePasswordRequest request = new UpdatePasswordRequest("wrongPass", "newPass");
        Account account = new Account("test@test.com", "hash", userId, "user");

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrongPass", "hash")).thenReturn(false);

        // When
        authService.updatePassword(userId, request);
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenUpdatingPasswordForNonExistentUser() {
        // Given
        String userId = "nonexistent";
        UpdatePasswordRequest request = new UpdatePasswordRequest("current", "new");

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        authService.updatePassword(userId, request);
    }

    @Test
    public void shouldHandleExceptionDuringPasswordUpdate() {
        // Given
        String userId = "user-123";
        UpdatePasswordRequest request = new UpdatePasswordRequest("current", "new");

        when(accountRepository.findByUserId(userId)).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        try {
            authService.updatePassword(userId, request);
            Assert.fail("Should have thrown exception");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(), "DB Error");
        }
    }

    // GET USER EMAIL TESTS

    @Test
    public void shouldGetUserEmailSuccessfully() {
        // Given
        String userId = "user-123";
        String email = "test@test.com";
        Account account = new Account(email, "hash", userId, "user");

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        // When
        String result = authService.getUserEmail(userId);

        // Then
        Assert.assertEquals(result, email);
        verify(accountRepository).findByUserId(userId);
    }

    @Test(expectedExceptions = InvalidCredentialsException.class)
    public void shouldThrowExceptionWhenGettingEmailForNonExistentUser() {
        // Given
        String userId = "nonexistent";

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        authService.getUserEmail(userId);
    }

    @Test
    public void shouldHandleExceptionDuringGetUserEmail() {
        // Given
        String userId = "user-123";

        when(accountRepository.findByUserId(userId)).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        try {
            authService.getUserEmail(userId);
            Assert.fail("Should have thrown exception");
        } catch (RuntimeException e) {
            Assert.assertEquals(e.getMessage(), "DB Error");
        }
    }

    // OAUTH LOGIN TESTS

    @Test(expectedExceptions = OAuthAccountException.class)
    public void shouldThrowExceptionWhenOAuthUserTriesToLoginWithPassword() {
        // Given
        LoginRequest request = new LoginRequest("oauth@gmail.com", "password");
        Account oauthAccount = new Account(
            "oauth@gmail.com",
            "user-id",
            "oauthUser",
            AuthProvider.GOOGLE,
            "google-123",
            null
        );

        when(accountRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(oauthAccount));

        // When
        authService.login(request);
    }

    @Test
    public void shouldLoginWithGoogleWhenAccountExistsByGoogleId() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .emailVerified(true)
            .name("John Doe")
            .pictureUrl("https://example.com/photo.jpg")
            .build();

        Account existingAccount = new Account(
            "test@gmail.com",
            "user-id-123",
            "existingUser",
            AuthProvider.GOOGLE,
            "google-123",
            "https://example.com/old-photo.jpg"
        );

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(existingAccount);

        // When
        User result = authService.loginWithGoogle(googleUserInfo);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), "user-id-123");
        Assert.assertEquals(result.getUsername(), "existingUser");
        Assert.assertFalse(result.isGuest());
    }

    @Test
    public void shouldUpdatePictureUrlWhenLoginWithGoogle() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .emailVerified(true)
            .name("John Doe")
            .pictureUrl("https://example.com/new-photo.jpg")
            .build();

        Account existingAccount = new Account(
            "test@gmail.com",
            "user-id-123",
            "existingUser",
            AuthProvider.GOOGLE,
            "google-123",
            "https://example.com/old-photo.jpg"
        );

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        authService.loginWithGoogle(googleUserInfo);

        // Then
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Assert.assertEquals(accountCaptor.getValue().getPictureUrl(), "https://example.com/new-photo.jpg");
    }

    @Test
    public void shouldLinkGoogleAccountToExistingLocalAccount() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("existing@test.com")
            .emailVerified(true)
            .name("John Doe")
            .pictureUrl("https://example.com/photo.jpg")
            .build();

        Account localAccount = new Account("existing@test.com", "hashedPassword", "user-id-123", "localUser");
        // Local account has AuthProvider.LOCAL by default

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(localAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = authService.loginWithGoogle(googleUserInfo);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), "user-id-123");

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        Assert.assertEquals(savedAccount.getGoogleId(), "google-123");
        Assert.assertEquals(savedAccount.getAuthProvider(), AuthProvider.GOOGLE);
    }

    @Test
    public void shouldCreateNewAccountWhenGoogleUserDoesNotExist() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-new-123")
            .email("newuser@gmail.com")
            .emailVerified(true)
            .name("New User")
            .pictureUrl("https://example.com/photo.jpg")
            .build();

        when(accountRepository.findByGoogleId("google-new-123")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());
        when(accountRepository.existsByUsername("NewUser")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = authService.loginWithGoogle(googleUserInfo);

        // Then
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isGuest());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        Assert.assertEquals(savedAccount.getEmail(), "newuser@gmail.com");
        Assert.assertEquals(savedAccount.getGoogleId(), "google-new-123");
        Assert.assertEquals(savedAccount.getAuthProvider(), AuthProvider.GOOGLE);
        Assert.assertNull(savedAccount.getPasswordHash());
    }

    @Test
    public void shouldGenerateUniqueUsernameWhenNameTaken() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .emailVerified(true)
            .name("John")
            .pictureUrl(null)
            .build();

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(accountRepository.existsByUsername("John")).thenReturn(true);
        when(accountRepository.existsByUsername(argThat(name -> name != null && name.startsWith("John_")))).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = authService.loginWithGoogle(googleUserInfo);

        // Then
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getUsername().startsWith("John_") || result.getUsername().startsWith("User_"));
    }

    @Test
    public void shouldUseEmailPrefixWhenGoogleNameIsNull() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("testuser@gmail.com")
            .emailVerified(true)
            .name(null)
            .pictureUrl(null)
            .build();

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("testuser@gmail.com")).thenReturn(Optional.empty());
        when(accountRepository.existsByUsername("testuser")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = authService.loginWithGoogle(googleUserInfo);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getUsername(), "testuser");
    }

    @Test
    public void shouldUseEmailPrefixWhenGoogleNameIsBlank() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("testuser@gmail.com")
            .emailVerified(true)
            .name("   ")
            .pictureUrl(null)
            .build();

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("testuser@gmail.com")).thenReturn(Optional.empty());
        when(accountRepository.existsByUsername("testuser")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = authService.loginWithGoogle(googleUserInfo);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getUsername(), "testuser");
    }

    @Test
    public void shouldHandleLongGoogleNames() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .emailVerified(true)
            .name("ThisIsAVeryLongNameThatExceedsFifteenCharacters")
            .pictureUrl(null)
            .build();

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("test@gmail.com")).thenReturn(Optional.empty());
        when(accountRepository.existsByUsername(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = authService.loginWithGoogle(googleUserInfo);

        // Then
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getUsername().length() <= 20); // max username length
    }

    @Test
    public void shouldNotUpdatePictureWhenSameUrl() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .emailVerified(true)
            .name("John")
            .pictureUrl("https://example.com/same-photo.jpg")
            .build();

        Account existingAccount = new Account(
            "test@gmail.com",
            "user-id-123",
            "existingUser",
            AuthProvider.GOOGLE,
            "google-123",
            "https://example.com/same-photo.jpg"
        );

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existingAccount));

        // When
        authService.loginWithGoogle(googleUserInfo);

        // Then
        verify(accountRepository, never()).save(any());
    }

    @Test(expectedExceptions = OAuthAccountException.class)
    public void shouldThrowExceptionWhenOAuthAccountTriesToUpdatePassword() {
        // Given
        String userId = "user-123";
        UpdatePasswordRequest request = new UpdatePasswordRequest("current", "new");
        Account oauthAccount = new Account(
            "oauth@gmail.com",
            userId,
            "oauthUser",
            AuthProvider.GOOGLE,
            "google-123",
            null
        );

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(oauthAccount));

        // When
        authService.updatePassword(userId, request);
    }

    @Test
    public void shouldLoginExistingGoogleAccountByEmail() {
        // Given
        GoogleUserInfo googleUserInfo = GoogleUserInfo.builder()
            .googleId("google-123")
            .email("test@gmail.com")
            .emailVerified(true)
            .name("John")
            .pictureUrl(null)
            .build();

        Account existingGoogleAccount = new Account(
            "test@gmail.com",
            "user-id-123",
            "existingGoogleUser",
            AuthProvider.GOOGLE,
            "google-123",
            null
        );

        when(accountRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
        when(accountRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(existingGoogleAccount));

        // When
        User result = authService.loginWithGoogle(googleUserInfo);

        // Then
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getId(), "user-id-123");
        Assert.assertEquals(result.getUsername(), "existingGoogleUser");
        verify(accountRepository, never()).save(any());
    }
}
