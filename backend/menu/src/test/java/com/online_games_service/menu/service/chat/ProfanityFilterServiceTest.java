package com.online_games_service.menu.service.chat;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ProfanityFilterServiceTest {

    private ProfanityFilterService profanityFilter;

    @BeforeMethod
    public void setUp() {
        profanityFilter = new ProfanityFilterService();
    }

    @Test
    public void shouldDetectProfanity() {
        Assert.assertTrue(profanityFilter.containsProfanity("This is a fuck test"));
        Assert.assertTrue(profanityFilter.containsProfanity("You're such a bitch"));
        Assert.assertTrue(profanityFilter.containsProfanity("DAMN this is bad"));
    }

    @Test
    public void shouldNotDetectCleanMessages() {
        Assert.assertFalse(profanityFilter.containsProfanity("Hello world"));
        Assert.assertFalse(profanityFilter.containsProfanity("Nice game!"));
        Assert.assertFalse(profanityFilter.containsProfanity("Let's play together"));
    }

    @Test
    public void shouldFilterProfanityWithAsterisks() {
        var result = profanityFilter.filter("This is a fuck test");
        Assert.assertTrue(result.wasFiltered());
        Assert.assertEquals(result.filteredMessage(), "This is a **** test");
    }

    @Test
    public void shouldFilterMultipleBadWords() {
        var result = profanityFilter.filter("What the fuck is this shit?");
        Assert.assertTrue(result.wasFiltered());
        Assert.assertTrue(result.filteredMessage().contains("****"));
        Assert.assertFalse(result.filteredMessage().contains("fuck"));
        Assert.assertFalse(result.filteredMessage().contains("shit"));
    }

    @Test
    public void shouldNotModifyCleanMessages() {
        var result = profanityFilter.filter("Hello, how are you?");
        Assert.assertFalse(result.wasFiltered());
        Assert.assertEquals(result.filteredMessage(), "Hello, how are you?");
    }

    @Test
    public void shouldHandleNullAndEmptyInput() {
        Assert.assertFalse(profanityFilter.containsProfanity(null));
        Assert.assertFalse(profanityFilter.containsProfanity(""));
        Assert.assertFalse(profanityFilter.containsProfanity("   "));

        var resultNull = profanityFilter.filter(null);
        Assert.assertFalse(resultNull.wasFiltered());

        var resultEmpty = profanityFilter.filter("");
        Assert.assertFalse(resultEmpty.wasFiltered());
    }

    @Test
    public void shouldDetectLeetSpeakVariants() {
        // f*ck with @ for a
        Assert.assertTrue(profanityFilter.containsProfanity("You're a b@stard"));
        // sh1t with 1 for i
        Assert.assertTrue(profanityFilter.containsProfanity("This is sh1t"));
    }

    @Test
    public void shouldBeCaseInsensitive() {
        Assert.assertTrue(profanityFilter.containsProfanity("FUCK"));
        Assert.assertTrue(profanityFilter.containsProfanity("FuCk"));
        Assert.assertTrue(profanityFilter.containsProfanity("SHIT"));
    }

    @Test
    public void shouldFilterPolishProfanity() {
        Assert.assertTrue(profanityFilter.containsProfanity("To jest kurwa"));
        
        var result = profanityFilter.filter("Ale cholera!");
        Assert.assertTrue(result.wasFiltered());
    }
}
