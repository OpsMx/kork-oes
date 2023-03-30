/*
 * Copyright 2021 Salesforce, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.kork.retrofit.exceptions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.netflix.spinnaker.config.ErrorConfiguration;
import com.netflix.spinnaker.config.RetrofitErrorConfiguration;
import com.netflix.spinnaker.kork.test.log.MemoryAppender;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// import
// org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedString;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {
      ErrorConfiguration.class,
      RetrofitErrorConfiguration.class,
      SpinnakerRetrofitExceptionHandlersTest.TestControllerConfiguration.class
    })
@TestPropertySource(properties = {"retrofit.enabled = false"})
class SpinnakerRetrofitExceptionHandlersTest {
  private static final String CUSTOM_MESSAGE = "custom message";
  @LocalServerPort int port;
  @Autowired TestRestTemplate restTemplate;
  private MemoryAppender memoryAppender;

  @BeforeEach
  void setup(TestInfo testInfo) {
    System.out.println("--------------- Test " + testInfo.getDisplayName());
    memoryAppender = new MemoryAppender(SpinnakerRetrofitExceptionHandlers.class);
  }

  /*@Test
  void testSpinnakerServerException() throws Exception {
    URI uri = getUri("/spinnakerServerException");
    ResponseEntity<String> entity = restTemplate.getForEntity(uri, String.class);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
    assertEquals(1, memoryAppender.countEventsForLevel(Level.ERROR));
  }*/

  /*@Test
  void testChainedSpinnakerServerException() throws Exception {
    URI uri = getUri("/chainedSpinnakerServerException");
    ResponseEntity<String> entity = restTemplate.getForEntity(uri, String.class);
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
    assertEquals(1, memoryAppender.countEventsForLevel(Level.ERROR));
    assertEquals(1, memoryAppender.search(CUSTOM_MESSAGE, Level.ERROR).size());
  }*/

  /*@ParameterizedTest(name = "testSpinnakerHttpException status = {0}")
  @ValueSource(ints = {403, 400, 500})
  void testSpinnakerHttpException(int status) throws Exception {
    URI uri = getUri("/spinnakerHttpException/" + String.valueOf(status));
    ResponseEntity<String> entity = restTemplate.getForEntity(uri, String.class);
    assertEquals(status, entity.getStatusCode().value());
    assertEquals(
        1,
        memoryAppender.countEventsForLevel(
            HttpStatus.resolve(status).is5xxServerError() ? Level.ERROR : Level.DEBUG));
  }*/

  /*@ParameterizedTest(name = "testChainedSpinnakerHttpException status = {0}")
  @ValueSource(ints = {403, 400, 500})
  void testChainedSpinnakerHttpException(int status) throws Exception {
    URI uri = getUri("/chainedSpinnakerHttpException/" + String.valueOf(status));
    ResponseEntity<String> entity = restTemplate.getForEntity(uri, String.class);
    assertEquals(status, entity.getStatusCode().value());
    assertEquals(
        1,
        memoryAppender.countEventsForLevel(
            HttpStatus.resolve(status).is5xxServerError() ? Level.ERROR : Level.DEBUG));
    assertEquals(
        1,
        memoryAppender
            .search(
                CUSTOM_MESSAGE,
                HttpStatus.resolve(status).is5xxServerError() ? Level.ERROR : Level.DEBUG)
            .size());
  }*/

  private URI getUri(String path) {
    return UriComponentsBuilder.fromHttpUrl("http://localhost/test-controller")
        .port(port)
        .path(path)
        .build()
        .toUri();
  }

  @Configuration
  @EnableAutoConfiguration
  static class TestControllerConfiguration {
    @EnableWebSecurity
    class WebSecurityConfig { // extends WebSecurityConfigurerAdapter {
      // @Override
      @Bean
      protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        // http.csrf().disable().headers().disable().authorizeRequests().anyRequest().permitAll();

        http.csrf()
            .disable()
            .headers()
            .disable()
            .authorizeHttpRequests(
                (requests) ->
                    requests
                        .requestMatchers(new AntPathRequestMatcher("*"))
                        .permitAll()
                        .anyRequest()
                        .authenticated());
        return http.build();
      }
    }

    @Bean
    TestController testController() {
      return new TestController();
    }
  }

  @RestController
  @RequestMapping("/test-controller")
  static class TestController {
    @GetMapping("/spinnakerServerException")
    void spinnakerServerException() {
      SpinnakerServerException spinnakerServerException = mock(SpinnakerServerException.class);
      when(spinnakerServerException.getMessage()).thenReturn("message");
      throw spinnakerServerException;
    }

    @GetMapping("/chainedSpinnakerServerException")
    void chainedSpinnakerServerException() {
      SpinnakerServerException spinnakerServerException = mock(SpinnakerServerException.class);
      when(spinnakerServerException.getMessage()).thenReturn("message");
      throw new SpinnakerServerException(CUSTOM_MESSAGE, spinnakerServerException);
    }

    @GetMapping("/spinnakerHttpException/{status}")
    void spinnakerHttpException(@PathVariable int status) {
      throw makeSpinnakerHttpException(status);
    }

    @GetMapping("/chainedSpinnakerHttpException/{status}")
    void chainedSpinnakerHttpException(@PathVariable int status) {
      throw new SpinnakerHttpException(CUSTOM_MESSAGE, makeSpinnakerHttpException(status));
    }

    SpinnakerHttpException makeSpinnakerHttpException(int status) {
      String url = "https://some-url";
      Response response =
          new Response(
              url,
              status,
              "arbitrary reason",
              List.of(),
              new TypedString("{ message: \"arbitrary message\" }"));
      return new SpinnakerHttpException(
          RetrofitError.httpError(url, response, new GsonConverter(new Gson()), Response.class));
    }
  }
}
