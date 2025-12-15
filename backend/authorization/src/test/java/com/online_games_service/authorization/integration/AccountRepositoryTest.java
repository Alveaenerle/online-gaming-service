package com.online_games_service.authorization.integration;

import com.online_games_service.authorization.model.Account;
import com.online_games_service.authorization.repository.AccountRepository;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
public class AccountRepositoryTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private AccountRepository accountRepository;

    @BeforeMethod
    public void cleanUp() {
        accountRepository.deleteAll();
    }

    @Test
    public void shouldSaveAndRetrieveAccount() {
        // Given
        Account account = new Account("jan@test.com", "hashedPassword123");

        // When
        Account savedAccount = accountRepository.save(account);

        // Then
        Assert.assertNotNull(savedAccount.getId());
        
        Optional<Account> retrieved = accountRepository.findById(savedAccount.getId());
        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(retrieved.get().getEmail(), "jan@test.com");
        Assert.assertNotNull(retrieved.get().getCreatedAt());
    }

    @Test
    public void shouldThrowExceptionOnDuplicateEmail() {
        // Given
        Account account1 = new Account("duplicate@test.com", "hash1");
        Account account2 = new Account("duplicate@test.com", "hash2");

        // When
        accountRepository.save(account1);

        // Then
        Assert.assertThrows(DuplicateKeyException.class, () -> {
            accountRepository.save(account2);
        });
    }

    @Test
    public void shouldThrowExceptionOnInvalidEmail() {
        // Given
        Account invalidAccount = new Account("not-an-email", "pass123");

        // When & Then
        Assert.assertThrows(ConstraintViolationException.class, () -> {
            accountRepository.save(invalidAccount);
        });
    }

    @Test
    public void shouldFindAccountByEmail() {
        // Given
        String email = "szukany@test.com";
        accountRepository.save(new Account(email, "someHash"));

        // When
        Optional<Account> found = accountRepository.findByEmail(email);

        // Then
        Assert.assertTrue(found.isPresent());
        Assert.assertEquals(found.get().getEmail(), email);
    }
}