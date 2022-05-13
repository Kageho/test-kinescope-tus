package com.example.kinscopetus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Config {
	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplateBuilder()
				.messageConverters(new MappingJackson2HttpMessageConverter(objectMapper()))
				.build();
	}

	@Bean
	ObjectMapper objectMapper() {
		return new Jackson2ObjectMapperBuilder().build();
	}
}