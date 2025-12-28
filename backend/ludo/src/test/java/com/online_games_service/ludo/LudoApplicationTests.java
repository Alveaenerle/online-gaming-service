package com.online_games_service.ludo;

import org.testng.annotations.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LudoApplicationTests extends AbstractTestNGSpringContextTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void main() {
		LudoApplication.main(new String[] {"--server.port=0"});
	}

}
