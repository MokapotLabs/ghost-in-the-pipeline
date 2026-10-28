package dev.ghost;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.*;
import java.util.Map;
import org.pipelineframework.config.pipeline.PipelineYamlConfigLoader;

/** Read the same native deployment binding as TPF; do not maintain a second model configuration. */
@ApplicationScoped
public class ModelSettings {
  private volatile Map<String, String> labels;

  public String model() {
    return models().get("TEV1");
  }

  public String model(String player) {
    return models().getOrDefault(player, model());
  }

  public Map<String, String> models() {
    if (labels == null)
      labels = Map.of("TEV1", readModel("model"), "TEV1_4B", readModel("model4b"),
          "JEV", readModel("ai-player-model-jev"));
    return labels;
  }

  private String readModel(String binding) {
    try {
      String path = System.getProperty("pipeline.config", System.getenv("PIPELINE_CONFIG"));
      var loader = new PipelineYamlConfigLoader();
      if (path != null && !path.isBlank())
        return loader
            .load(Path.of(path))
            .connectors()
            .get(binding)
            .config()
            .get("model")
            .toString();
      try (var stream = getClass().getClassLoader().getResourceAsStream("pipeline.yaml")) {
        return loader.load(stream).connectors().get(binding).config().get("model").toString();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read model deployment binding", e);
    }
  }
}
