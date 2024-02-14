/*
 * Copyright 2024 OpsMx.
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

package com.netflix.spinnaker.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.util.List;

/**
 * OP-21771:
 * This configuration helps to decode the base64 response of /v3/api-docs and makes Swagger UI load the API docs properly. *
 *
 * At the service(child services to Kork) level, while registering converters, somehow the ordering of HttpMessageConverters is getting disturbed.
 * This configuration aligns it properly and there by fix decoding issue of swagger responses.
 *
 * ref docs: https://github.com/springdoc/springdoc.github.io/issues/52
 */
@Configuration
public class ApiWebMvcConfiguration extends DelegatingWebMvcConfiguration {

  @Autowired
  Jackson2ObjectMapperBuilder jacksonBuilder;

  @Override
  protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    var applicationContext = this.getApplicationContext();
    if (applicationContext != null) {
      jacksonBuilder.applicationContext(applicationContext);
    }
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(new MappingJackson2HttpMessageConverter(jacksonBuilder.build()));
  }
}
