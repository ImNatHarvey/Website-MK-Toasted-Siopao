package com.toastedsiopao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

	@Value("${file.upload-dir}")
	private String uploadDir;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// This makes files in the "mk-uploads" directory accessible via
		// "http://localhost:8080/img/uploads/filename.png"

		Path uploadPath = Paths.get(uploadDir);
		String uploadPathAbsolute = uploadPath.toFile().getAbsolutePath();
		String resourceLocation = "file:/" + uploadPathAbsolute + "/";

		registry.addResourceHandler("/img/uploads/**").addResourceLocations(resourceLocation);

		log.info("Configured resource handler for /img/uploads/** to serve from: {}", resourceLocation);
	}
}