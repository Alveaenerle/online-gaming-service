package com.online_games_service.tests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HelloControllerWebTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MockMvc mockMvc;

    @DataProvider
    public Object[][] greetingCases() {
        return new Object[][]{
                {"World", "Hello World", 11},
                {"Kuba", "Hello Kuba", 10},
                {"Player1", "Hello Player1", 13}
        };
    }

    @Test(dataProvider = "greetingCases")
    public void shouldReturnGreetingForGivenName(String name,
                                                 String expectedMessage,
                                                 int expectedLength) throws Exception {

        mockMvc.perform(get("/hello")
                        .param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(expectedMessage))
                .andExpect(jsonPath("$.length").value(expectedLength));
    }

    @Test
    public void shouldUseDefaultNameWhenNameIsMissing() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello World"))
                .andExpect(jsonPath("$.length").value(11));
    }
}
