package llmusagelimits.backend;

import java.util.List;

import llmusagelimits.model.InferenceResult;
import llmusagelimits.model.Model;

// The seam to the model servers (out of scope). Throws on failure.
public interface InferenceBackend {
    InferenceResult generate(Model model, List<String> messages);
}
