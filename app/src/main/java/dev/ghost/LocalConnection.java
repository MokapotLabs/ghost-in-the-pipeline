package dev.ghost;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.pipelineframework.connector.*;
import org.pipelineframework.connector.decision.jev.AuthenticatedJevConnection;

@ApplicationScoped
public class LocalConnection implements ConnectionResolver {
  private final HttpClient client =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  private final Supplier<Optional<String>> apiKeySupplier;

  public LocalConnection() {
    this(() -> Optional.ofNullable(System.getenv("OPENROUTER_API_KEY")));
  }

  LocalConnection(Supplier<Optional<String>> apiKeySupplier) {
    this.apiKeySupplier = Objects.requireNonNull(apiKeySupplier);
  }

  public <C extends ResolvedConnection> CompletionStage<C> resolve(
      ConnectionResolutionRequest<C> r) {
    try {
      String reference = r.reference().value();
      if ((!reference.equals("local-ollama") && !reference.equals("openrouter-primary"))
          || r.connectionType() != AuthenticatedJevConnection.class)
        throw new ConnectionResolutionException("Unsupported game model connection");
      String key = reference.equals("openrouter-primary")
          ? apiKeySupplier.get().map(String::trim).filter(value -> !value.isEmpty())
              .orElseThrow(() -> new ConnectionResolutionException(
                  ConnectionResolutionException.Kind.AUTHENTICATION_REQUIRED,
                  "OpenRouter credentials are unavailable in the application host"))
          : "ollama";
      return CompletableFuture.completedFuture(
          r.connectionType().cast(AuthenticatedJevConnection.bearer(client, key)));
    } catch (RuntimeException failure) {
      return CompletableFuture.failedFuture(failure);
    }
  }
}
