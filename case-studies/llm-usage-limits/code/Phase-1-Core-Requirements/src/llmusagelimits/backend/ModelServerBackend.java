package llmusagelimits.backend;

import java.util.List;

import llmusagelimits.model.InferenceResult;
import llmusagelimits.model.Model;

// Calls the model-serving cluster.
public class ModelServerBackend implements InferenceBackend {
    @Override
    public InferenceResult generate(Model model, List<String> messages) {
        // stub: real code would send the messages to the model's server pool and read the
        // token counts from its response; a timeout or server error should throw.
        long inputTokens = 0;
        for (String m : messages) inputTokens += (m.length() + 3) / 4;
        String reply = "(" + model + " reply to " + messages.size() + " message(s))";
        System.out.println("[" + model + "] " + inputTokens + " input tokens -> " + reply);
        return new InferenceResult(reply, inputTokens, 50);
    }
}
