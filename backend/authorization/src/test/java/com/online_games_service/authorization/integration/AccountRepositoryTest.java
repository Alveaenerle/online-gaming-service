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
        String userID = "123";
        String username = "username";
        Account account = new Account("jan@test.com", "hashedPassword123", userID, username);

        Account savedAccount = accountRepository.save(account);

        Assert.assertNotNull(savedAccount.getId());
        Optional<Account> retrieved = accountRepository.findById(savedAccount.getId());
        Assert.assertTrue(retrieved.isPresent());
        Assert.assertEquals(retrieved.get().getEmail(), "jan@test.com");
    }

    @Test
    public void shouldThrowExceptionOnDuplicateEmail() {
        String userID1 = "1234";
        String userID2 = "12345";
        String username = "username";
        Account account1 = new Account("duplicate@test.com", "hash1", userID1, username);
        Account account2 = new Account("duplicate@test.com", "hash2", userID2, username);

        accountRepository.save(account1);

        Assert.assertThrows(DuplicateKeyException.class, () -> {
            accountRepository.save(account2);
        });
    }

    @Test
    public void shouldThrowExceptionOnInvalidEmail() {
        String userID = "123";
        String username = "username";
        Account invalidAccount = new Account("not-an-email", "pass123", userID, username);

        Assert.assertThrows(ConstraintViolationException.class, () -> {
            accountRepository.save(invalidAccount);
        });
    }

    @Test
    public void shouldFindAccountByEmail() {
        String userID = "123";
        String username = "username";
        String email = "szukany@test.com";
        accountRepository.save(new Account(email, "someHash", userID, username));

        Optional<Account> found = accountRepository.findByEmail(email);

        Assert.assertTrue(found.isPresent());
        Assert.assertEquals(found.get().getEmail(), email);
    }

    @Test
    public void shouldThrowExceptionWhenLinkingTwoAccountsToSameUser() {        
        String userId = "user_id_12345"; 
        Account account1 = new Account("email1@test.com", "pass1", userId, "username");
        Account account2 = new Account("email2@test.com", "pass2", userId, "username"); 

        accountRepository.save(account1);

        Assert.assertThrows(DuplicateKeyException.class, () -> {
            accountRepository.save(account2);
        });
    }
}