package net.engineeringdigest.journalApp;

import net.engineeringdigest.journalApp.services.*;
import net.engineeringdigest.journalApp.utils.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JournalAppApplicationTests {

	@MockBean
	private JwtUtil jwtUtil;

	@MockBean
	private RedisService redisService;

	@MockBean
	private EmailService emailService;

	@MockBean
	private GeminiService geminiService;

	@MockBean
	private BiweeklyReportService biweeklyReportService;

	@Test
	void contextLoads() {
	}
}