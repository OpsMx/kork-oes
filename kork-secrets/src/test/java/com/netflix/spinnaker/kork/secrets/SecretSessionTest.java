package com.netflix.spinnaker.kork.secrets;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.netflix.spinnaker.kork.secrets.engines.NoopSecretEngine;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SecretSessionTest {

  @Mock private SecretEngineRegistry secretEngineRegistry;
  private SecretEngine secretEngine;
  private List<SecretEngine> secretEngineList = new ArrayList<>();
  private SecretManager secretManager;
  private SecretSession secretSession;

  @BeforeAll
  public void setup() {
    MockitoAnnotations.initMocks(this);

    secretManager = spy(new SecretManager(secretEngineRegistry));
    doCallRealMethod().when(secretManager).decrypt(any());

    secretEngine = spy(new NoopSecretEngine());
    doCallRealMethod().when(secretEngine).decrypt(any(EncryptedSecret.class));

    secretEngineList.add(secretEngine);
    secretSession = new SecretSession(secretManager);
    addTestValuesToSecretSessionCaches();

    when(secretEngineRegistry.getSecretEngineList()).thenReturn(secretEngineList);
    when(secretEngineRegistry.getEngine("noop")).thenReturn(secretEngine);
  }

  private void addTestValuesToSecretSessionCaches() {
    secretSession.addCachedSecret("encrypted:noop!f:test!k:key", "decrypted");
    secretSession.addCachedSecretFile("encrypted:noop!f:file", Paths.get("decryptedFile"));
  }

  @Test
  public void decryptReturnsSecretFromCache() {
    String decrypted = secretSession.decrypt("encrypted:noop!f:test!k:key");
    Assertions.assertEquals("decrypted", decrypted);
  }

  @Test
  public void decryptAddsToCacheOnCacheMiss() {
    Assertions.assertEquals(1, secretSession.secretCache.size());
    String decrypted = secretSession.decrypt("encrypted:noop!f:unknown!v:test");
    Assertions.assertEquals("test", decrypted);
    Assertions.assertEquals(
        "test", secretSession.secretCache.get("encrypted:noop!f:unknown!v:test"));
    Assertions.assertEquals(2, secretSession.secretCache.size());
  }

  @Test
  public void decryptAsFileReturnsSecretFromCache() {
    String decryptedPath = secretSession.decryptAsFile("encrypted:noop!f:file").toString();
    Assertions.assertEquals("decryptedFile", decryptedPath);
  }

  @Test
  public void decryptAsFileAddsToCacheOnCacheMiss() {
    doReturn(Paths.get("tempFile")).when(secretManager).decryptAsFile(any());
    Assertions.assertEquals(1, secretSession.secretFileCache.size());
    Path decrypted = secretSession.decryptAsFile("encrypted:noop!f:unknown");
    Assertions.assertEquals(2, secretSession.secretFileCache.size());
  }

  @Test
  public void clearCachedSecretsShouldClearAllCaches() {
    Assertions.assertEquals(1, secretSession.secretCache.size());
    Assertions.assertEquals(1, secretSession.secretFileCache.size());
    secretSession.clearCachedSecrets();
    Assertions.assertEquals(0, secretSession.secretCache.size());
    Assertions.assertEquals(0, secretSession.secretFileCache.size());
    verify(secretEngine, times(1)).clearCache();
  }
}
