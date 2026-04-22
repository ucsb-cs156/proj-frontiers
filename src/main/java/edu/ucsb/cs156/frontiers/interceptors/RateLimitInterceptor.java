package edu.ucsb.cs156.frontiers.interceptors;

import org.springframework.http.client.ClientHttpRequestInterceptor;

@FunctionalInterface
public interface RateLimitInterceptor extends ClientHttpRequestInterceptor {}
