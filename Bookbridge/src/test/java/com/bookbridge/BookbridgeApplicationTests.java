package com.bookbridge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EntityScan("com.bookbridge.model")
class BookbridgeApplicationTests {

	@Test
	void contextLoads() {
	}

}
