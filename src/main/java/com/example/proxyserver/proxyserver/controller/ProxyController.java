package com.example.proxyserver.proxyserver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

	private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);
	private final RestTemplate restTemplate;
	private final BlockingQueue<Runnable> requestQueue = new LinkedBlockingQueue<>();

	@Value("${default.target.url}")
	private String defaultTargetUrl; // Injected from application.properties

	public ProxyController(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;

		// Start a thread to process requests sequentially
		new Thread(() -> {
			while (true) {
				try {
					Runnable task = requestQueue.take();
					task.run();
				} catch (InterruptedException e) {
					logger.error("Error processing request", e);
					Thread.currentThread().interrupt();
				}
			}
		}).start();
	}

	@GetMapping
	public String proxyGet(@RequestParam(required = false) String url) {
		final String[] responseHolder = new String[1]; // To hold the response
		requestQueue.add(() -> {
			String targetUrl = url != null ? url : defaultTargetUrl; // Use injected default URL
			try {
				String response = restTemplate.getForObject(targetUrl, String.class);
				responseHolder[0] = response; // Store the response
				logger.info("Response: {}", response);
			} catch (Exception e) {
				logger.error("Error fetching URL: {}", targetUrl, e);
				responseHolder[0] = "Error fetching URL: " + targetUrl;
			}
		});

		// Wait for the request to be processed
		waitForResponse(responseHolder);

		return responseHolder[0]; // Return the response to the client
	}

	@PostMapping
	public String proxyPost(@RequestParam(required = false) String url, @RequestBody String body) {
		final String[] responseHolder = new String[1]; // To hold the response
		requestQueue.add(() -> {
			String targetUrl = url != null ? url : defaultTargetUrl; // Use injected default URL
			try {
				String response = restTemplate.postForObject(targetUrl, body, String.class);
				responseHolder[0] = response; // Store the response
				logger.info("Response: {}", response);
			} catch (Exception e) {
				logger.error("Error posting to URL: {}", targetUrl, e);
				responseHolder[0] = "Error posting to URL: " + targetUrl;
			}
		});

		// Wait for the request to be processed
		waitForResponse(responseHolder);

		return responseHolder[0]; // Return the response to the client
	}

	private void waitForResponse(String[] responseHolder) {
		while (responseHolder[0] == null) {
			try {
				Thread.sleep(10); // Small delay to avoid busy waiting
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				responseHolder[0] = "Error: Interrupted while waiting for response";
			}
		}
	}

	@GetMapping("/test")
	public String test() {
		return "Proxy is running!";
	}
}