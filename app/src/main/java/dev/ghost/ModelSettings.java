package dev.ghost;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.*;
import org.pipelineframework.config.pipeline.PipelineYamlConfigLoader;

/** Read the same native deployment binding as TPF; do not maintain a second model configuration. */
@ApplicationScoped
public class ModelSettings {
  private volatile String label;

  public String model() {
    if (label == null) label = readModel();
    return label;
  }

  private String readModel() {
    try {
      String path = System.getProperty("pipeline.config", System.getenv("PIPELINE_CONFIG"));
      var loader = new PipelineYamlConfigLoader();
      if (path != null && !path.isBlank())
        return loader
            .load(Path.of(path))
            .connectors()
            .get("model")
            .config()
            .get("model")
            .toString();
      try (var stream = getClass().getClassLoader().getResourceAsStream("pipeline.yaml")) {
        return loader.load(stream).connectors().get("model").config().get("model").toString();
      }
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read model deployment binding", e);
    }
  }
}
