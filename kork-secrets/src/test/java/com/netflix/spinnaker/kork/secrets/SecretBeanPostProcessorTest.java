package com.netflix.spinnaker.kork.secrets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

public class SecretBeanPostProcessorTest {

  @Mock private ConfigurableApplicationContext applicationContext;

  @Mock private ConfigurableEnvironment environment;

  private SecretBeanPostProcessor secretBeanPostProcessor;
  private MutablePropertySources mutablePropertySources = new MutablePropertySources();

  private PropertySource propertySource =
      new PropertySource("testPropertySource") {
        @Override
        public Object getProperty(String name) {
          return null;
        }
      };

  private EnumerablePropertySource enumerablePropertySource =
      new EnumerablePropertySource("testEnumerableSource") {
        @Override
        public String[] getPropertyNames() {
          return new String[0];
        }

        @Override
        public Object getProperty(String name) {
          return null;
        }
      };

  @BeforeEach
  public void setup() {
    mutablePropertySources.addLast(propertySource);
    mutablePropertySources.addLast(enumerablePropertySource);

    MockitoAnnotations.initMocks(this);
    when(applicationContext.getEnvironment()).thenReturn(environment);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
  }

  @Test
  public void secretManagerBeanShouldGetProcessed() {
    secretBeanPostProcessor = new SecretBeanPostProcessor(applicationContext, null);
    verify(applicationContext, times(1)).getEnvironment();
  }

  @Test
  public void replaceEnumerableSourceWithSecretAwareSourceInSecretManagerBean() {
    assertTrue(
        mutablePropertySources.get("testEnumerableSource") instanceof EnumerablePropertySource);
    assertFalse(
        mutablePropertySources.get("testPropertySource") instanceof EnumerablePropertySource);

    secretBeanPostProcessor = new SecretBeanPostProcessor(applicationContext, null);

    assertTrue(
        mutablePropertySources.get("testEnumerableSource") instanceof SecretAwarePropertySource);
    assertFalse(
        mutablePropertySources.get("testPropertySource") instanceof SecretAwarePropertySource);
  }
}
