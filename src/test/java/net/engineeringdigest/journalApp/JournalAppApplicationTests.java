package net.engineeringdigest.journalApp;


import net.engineeringdigest.journalApp.config.TestMailConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
@Import(TestMailConfig.class)
@Disabled("Disabled for CI/CD - context application bean issues")
class JournalAppApplicationTests {

	@Disabled("Disabled for CI/CD - requires database connection")
	@Test
	void contextLoads() {
	}

}
