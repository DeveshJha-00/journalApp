package net.engineeringdigest.journalApp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JournalAppApplicationTests {

	@Disabled("Disabled for CI/CD - requires database connection")
	@Test
	void contextLoads() {
	}

}
