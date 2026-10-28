package dev.ghost;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.pipelineframework.connector.*;
import org.pipelineframework.connector.decision.jev.AuthenticatedJevConnection;

class LocalConnectionTest {
  private ConnectionResolutionRequest<AuthenticatedJevConnection> request(String reference) {
    return new ConnectionResolutionRequest<>(new ConnectionRef(reference),
        AuthenticatedJevConnection.class, ConnectorExecutionContext.empty());
  }

  @Test
  void localPlayDoesNotRequireHostedCredentials() {
    var resolver = new LocalConnection(() -> { throw new AssertionError("Remote key requested"); });
    assertNotNull(resolver.resolve(request("local-ollama")).toCompletableFuture().join());
  }

  @Test
  void hostedBindingResolvesWithHostOwnedKey() {
    var resolver = new LocalConnection(() -> Optional.of(" example-test-key "));
    assertNotNull(resolver.resolve(request("openrouter-primary")).toCompletableFuture().join());
  }

  @Test
  void missingHostedCredentialsFailWithoutAffectingLocalBinding() {
    for (Optional<String> key : java.util.List.of(Optional.<String>empty(), Optional.of("  "))) {
      var resolver = new LocalConnection(() -> key);
      var failure = assertThrows(CompletionException.class,
          () -> resolver.resolve(request("openrouter-primary")).toCompletableFuture().join());
      var cause = assertInstanceOf(ConnectionResolutionException.class, failure.getCause());
      assertEquals(ConnectionResolutionException.Kind.AUTHENTICATION_REQUIRED, cause.kind());
      assertNotNull(resolver.resolve(request("local-ollama")).toCompletableFuture().join());
    }
  }

  @Test
  void rejectsUnknownReferencesAndUnsupportedCapabilitiesBeforeReadingKey() {
    var resolver = new LocalConnection(() -> { throw new AssertionError("Key requested"); });
    assertThrows(CompletionException.class,
        () -> resolver.resolve(request("unknown")).toCompletableFuture().join());
    assertThrows(CompletionException.class,
        () -> resolver.resolve(new ConnectionResolutionRequest<>(new ConnectionRef("openrouter-primary"),
            ResolvedConnection.class, ConnectorExecutionContext.empty())).toCompletableFuture().join());
  }
}
