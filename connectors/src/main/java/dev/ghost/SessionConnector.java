package dev.ghost;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.*;
import org.pipelineframework.connector.*;

@ApplicationScoped
public class SessionConnector implements ConnectorProvider<ConnectorConfigurationDocument> {
  @Inject Session session;

  @Inject
  @org.eclipse.microprofile.config.inject.ConfigProperty(
      name = "ghost.pace-ms",
      defaultValue = "160")
  int paceMs;

  public ConnectorProviderId id() {
    return ConnectorProviderId.of("ghost.session");
  }

  public ConnectorProviderVersion version() {
    return new ConnectorProviderVersion(1, 0);
  }

  public Collection<? extends ConnectorOperation> operations() {
    return List.of(new Control(this), new Publish(this));
  }

  public record EmptyConfig() {}

  static final class Control implements QueryOperation<RunState, EmptyConfig, RunState> {
    private final SessionConnector host;

    Control(SessionConnector h) {
      host = h;
    }

    public String id() {
      return "control";
    }

    public Optional<ConnectorConfigSchema<EmptyConfig>> configurationSchema() {
      return Optional.of(ConnectorConfigSchema.record(EmptyConfig.class, "ghost.control", 1));
    }

    public CompletionStage<QueryOutcome<RunState>> query(
        QueryInvocation<RunState, EmptyConfig, RunState> i) {
      long delay = i.input().player().equals("HUMAN") ? 0 : Math.clamp(host.paceMs, 0, 2000);
      return CompletableFuture.supplyAsync(
          () -> new QueryOutcome.Found<>(host.session.control(i.input())),
          CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS));
    }
  }

  static final class Publish implements CommandOperation<RunState, EmptyConfig, RunState> {
    private final SessionConnector host;

    Publish(SessionConnector h) {
      host = h;
    }

    public String id() {
      return "publish";
    }

    public Optional<ConnectorConfigSchema<EmptyConfig>> configurationSchema() {
      return Optional.of(ConnectorConfigSchema.record(EmptyConfig.class, "ghost.publish", 1));
    }

    public CompletionStage<CommandOutcome<RunState>> dispatch(
        CommandInvocation<RunState, EmptyConfig> i) {
      return CompletableFuture.completedFuture(
          new CommandOutcome.Succeeded<>(
              host.session.publish(i.input()), CommandConfirmation.none(), List.of()));
    }
  }
}
