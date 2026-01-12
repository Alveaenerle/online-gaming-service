package com.online_games_service.menu.service.chat;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Service for filtering profanity from chat messages.
 * Uses a dictionary-based approach with regex matching.
 */
@Service
public class ProfanityFilterService {
    
    // Common profanity words - can be extended or loaded from a file
    private static final Set<String> BAD_WORDS = Set.of(
        "fuck", "shit", "ass", "bitch", "bastard", "damn", "crap", "piss",
        "dick", "cock", "pussy", "cunt", "whore", "slut", "fag", "faggot",
        "nigger", "nigga", "retard", "idiot", "moron", "stupid",
        "kurwa", "chuj", "pizda", "skurwysyn", "dupa", "cholera", "kurwo"  // Polish
    );
    
    // Pre-compiled patterns for efficiency
    private final Pattern profanityPattern;
    
    public ProfanityFilterService() {
        // Build regex pattern that matches whole words (case-insensitive)
        // Also handles common letter substitutions (l33t speak)
        StringBuilder patternBuilder = new StringBuilder("\\b(");
        boolean first = true;
        for (String word : BAD_WORDS) {
            if (!first) {
                patternBuilder.append("|");
            }
            // Create pattern with common substitutions
            patternBuilder.append(createFlexiblePattern(word));
            first = false;
        }
        patternBuilder.append(")\\b");
        this.profanityPattern = Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }
    
    /**
     * Creates a flexible regex pattern that handles common letter substitutions.
     */
    private String createFlexiblePattern(String word) {
        return word
            .replace("a", "[a@4]")
            .replace("e", "[e3]")
            .replace("i", "[i1!]")
            .replace("o", "[o0]")
            .replace("s", "[s$5]")
            .replace("t", "[t7]")
            .replace("l", "[l1]");
    }
    
    /**
     * Checks if a message contains profanity.
     */
    public boolean containsProfanity(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        return profanityPattern.matcher(message).find();
    }
    
    /**
     * Filters profanity from a message, replacing bad words with asterisks.
     * Returns a result containing the filtered message and whether it was modified.
     */
    public FilterResult filter(String message) {
        if (message == null || message.isBlank()) {
            return new FilterResult(message, false);
        }
        
        var matcher = profanityPattern.matcher(message);
        StringBuilder result = new StringBuilder();
        boolean wasFiltered = false;
        
        while (matcher.find()) {
            wasFiltered = true;
            String replacement = "*".repeat(matcher.group().length());
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return new FilterResult(result.toString(), wasFiltered);
    }
    
    /**
     * Result of filtering a message.
     */
    public record FilterResult(String filteredMessage, boolean wasFiltered) {}
}
