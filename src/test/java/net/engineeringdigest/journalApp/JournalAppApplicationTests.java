package net.engineeringdigest.journalApp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class JournalAppApplicationTests {

	@Disabled("Disabled for CI/CD - requires database connection")
	@Test
	void contextLoads() {
	}

}
