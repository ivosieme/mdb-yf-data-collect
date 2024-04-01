package com.example.mdbspringboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.mockito.Mockito.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class MdbSpringBootApplicationTests {

	@MockBean
	private RabbitTemplate rabbitTemplate;

	@Test
	void contextLoads() {
	}

}
