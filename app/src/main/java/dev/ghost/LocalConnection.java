package dev.ghost;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.http.HttpClient;
import java.util.concurrent.*;
import org.pipelineframework.connector.*;
import org.pipelineframework.connector.decision.jev.AuthenticatedJevConnection;

@ApplicationScoped
public class LocalConnection implements ConnectionResolver {
  private final HttpClient client =
      HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

  public <C extends ResolvedConnection> CompletionStage<C> resolve(
      ConnectionResolutionRequest<C> r) {
    if (!r.reference().toString().contains("local-ollama")
        || r.connectionType() != AuthenticatedJevConnection.class)
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("unknown-local-connection"));
    return CompletableFuture.completedFuture(
        r.connectionType().cast(AuthenticatedJevConnection.bearer(client, "ollama")));
  }
}
