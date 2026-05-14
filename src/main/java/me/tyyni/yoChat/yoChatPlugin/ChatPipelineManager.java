package me.tyyni.yoChat.yoChatPlugin;

import lombok.Getter;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatContext;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.ChatPipelineStep;
import me.tyyni.yoChat.yoChatAPI.chatPipeline.Stage;
import me.tyyni.yoChat.yoChatAPI.YoChatAPI;
import me.tyyni.yoChat.yoChatPlugin.steps.ChannelStep;
import me.tyyni.yoChat.yoChatPlugin.steps.FilterStep;
import me.tyyni.yoChat.yoChatPlugin.steps.FinalizeStep;
import me.tyyni.yoChat.yoChatPlugin.steps.FormatStep;
import me.tyyni.yoChat.yoChatPlugin.steps.MentionStep;
import me.tyyni.yoChat.yoChatPlugin.steps.MuteStep;
import me.tyyni.yoChat.yoChatPlugin.steps.PlaceholderStep;
import me.tyyni.yoChat.yoChatPlugin.steps.ViewerStep;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime registry and executor for YoChat's chat pipeline.
 *
 * <p>This type is primarily used through {@link YoChatAPI}, but it is also exposed
 * for integrations that need deeper access to the registered pipeline structure.</p>
 */
public class ChatPipelineManager {
    @Getter
    private static ChatPipelineManager instance;
    private static final Comparator<RegisteredPipelineStep> STEP_ORDER =
            Comparator.comparingInt(RegisteredPipelineStep::priority)
                    .thenComparingLong(RegisteredPipelineStep::registrationOrder);

    public ChatPipelineManager() {
        instance = this;
        registerDefaults();
    }

    private final Map<Stage, List<RegisteredPipelineStep>> pipelineSteps = new ConcurrentHashMap<>();
    private final AtomicLong registrationCounter = new AtomicLong();

    /**
     * Registers a step into the given stage using the step's default priority.
     *
     * @param stage the pipeline stage
     * @param step the step to register
     */
    public void registerStep(Stage stage, @NonNull ChatPipelineStep step) {
        registerStep(stage, step, step.getPriority());
    }

    /**
     * Registers a step into the given stage with an explicit priority.
     *
     * @param stage the pipeline stage
     * @param step the step to register
     * @param priority the explicit execution priority
     */
    public void registerStep(Stage stage, @NonNull ChatPipelineStep step, int priority) {
        List<RegisteredPipelineStep> registeredSteps = pipelineSteps.computeIfAbsent(stage, key -> new CopyOnWriteArrayList<>());
        boolean alreadyRegistered = registeredSteps.stream().anyMatch(registered -> registered.step() == step);
        if (alreadyRegistered) {
            return;
        }

        registeredSteps.add(new RegisteredPipelineStep(
                stage,
                step,
                priority,
                step.isAsyncSafe(),
                registrationCounter.getAndIncrement()
        ));
        registeredSteps.sort(STEP_ORDER);
        ConfigManager.getInstance().debug(
                "Registered chat pipeline step successfully: %s -> %s (priority=%d, asyncSafe=%s)",
                stage,
                step.getClass().getName(),
                priority,
                step.isAsyncSafe()
        );
    }

    public void execute(ChatContext context) {
        for (Stage stage : Stage.values()) {
            execute(stage, context);
            if (context.isCancelled()) return;
        }
    }

    /**
     * Executes a single pipeline stage against the given context.
     *
     * @param stage the stage to execute
     * @param context the chat context to mutate
     */
    public void execute(Stage stage, ChatContext context) {
        List<RegisteredPipelineStep> steps = pipelineSteps.get(stage);
        if (steps == null) {
            return;
        }

        for (RegisteredPipelineStep step : steps) {
            executeStep(step, context);
            if (context.isCancelled()) {
                return;
            }
        }
    }

    /**
     * Returns the registered steps for a single stage.
     *
     * @param stage the stage to inspect
     * @return an immutable snapshot of registered steps for that stage
     */
    public List<RegisteredPipelineStep> getSteps(Stage stage) {
        List<RegisteredPipelineStep> steps = pipelineSteps.get(stage);
        return steps == null ? List.of() : List.copyOf(steps);
    }

    /**
     * Returns all registered pipeline steps grouped by stage.
     *
     * @return a snapshot of the internal registration map
     */
    public Map<Stage, List<RegisteredPipelineStep>> getRegisteredSteps() {
        Map<Stage, List<RegisteredPipelineStep>> snapshot = new ConcurrentHashMap<>();
        for (Stage stage : Stage.values()) {
            snapshot.put(stage, getSteps(stage));
        }
        return snapshot;
    }

    /**
     * Returns only the raw step implementations grouped by stage.
     *
     * @return a snapshot of registered step implementations
     */
    public Map<Stage, List<ChatPipelineStep>> getPipelineSteps() {
        Map<Stage, List<ChatPipelineStep>> snapshot = new ConcurrentHashMap<>();
        for (Stage stage : Stage.values()) {
            List<ChatPipelineStep> steps = new ArrayList<>();
            for (RegisteredPipelineStep registeredStep : getSteps(stage)) {
                steps.add(registeredStep.step());
            }
            snapshot.put(stage, List.copyOf(steps));
        }
        return snapshot;
    }

    /**
     * Removes every step from the given stage.
     *
     * @param stage the stage to clear
     */
    public void clearSteps(Stage stage) {
        List<RegisteredPipelineStep> steps = pipelineSteps.get(stage);
        if (steps != null) {
            steps.clear();
        }
    }

    /**
     * Removes every registered step from every stage.
     */
    public void clearAllSteps() {
        pipelineSteps.values().forEach(List::clear);
    }

    /**
     * Removes the given step instance from every stage where it is registered.
     *
     * @param step the step instance to remove
     */
    public void unregisterStep(ChatPipelineStep step) {
        pipelineSteps.values().forEach(registeredList -> registeredList.removeIf(registered -> registered.step() == step));
    }

    /**
     * Removes the given step instance from a single stage.
     *
     * @param stage the stage to remove from
     * @param step the step instance to remove
     */
    public void unregisterStep(Stage stage, ChatPipelineStep step) {
        List<RegisteredPipelineStep> steps = pipelineSteps.get(stage);
        if (steps != null) {
            steps.removeIf(registered -> registered.step() == step);
        }
    }

    private void executeStep(RegisteredPipelineStep registeredStep, ChatContext context) {
        if (registeredStep.asyncSafe() || Bukkit.isPrimaryThread()) {
            registeredStep.step().process(context);
            return;
        }

        try {
            callSync(() -> {
                registeredStep.step().process(context);
                return null;
            });
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            context.setCancelled(true);
            YoChatAPI.getPlugin().getLogger().warning("Chat pipeline step interrupted: " + registeredStep.step().getClass().getName());
        } catch (ExecutionException ex) {
            context.setCancelled(true);
            YoChatAPI.getPlugin().getLogger().severe("Chat pipeline step failed: " + registeredStep.step().getClass().getName() + " -> " + ex.getCause());
        }
    }

    private <T> void callSync(Callable<T> task) throws ExecutionException, InterruptedException {
        if (Bukkit.isPrimaryThread()) {
            try {
                task.call();
                return;
            } catch (ExecutionException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ExecutionException(ex);
            }
        }

        Bukkit.getScheduler().callSyncMethod(YoChatAPI.getPlugin(), task).get();
    }

    private void registerDefaults() {
        registerStep(Stage.PRE, new MuteStep(), 0);
        registerStep(Stage.PROCESS, new FilterStep(), 0);
        registerStep(Stage.PLACEHOLDER, new PlaceholderStep(), 0);
        registerStep(Stage.CHANNEL, new ChannelStep(), 0);
        registerStep(Stage.VIEWERS, new ViewerStep(), 0);
        registerStep(Stage.FORMAT, new FormatStep(), 0);
        registerStep(Stage.POST, new FinalizeStep(), 0);
        registerStep(Stage.POST, new MentionStep(), 100);
    }

    /**
     * Immutable metadata for a single registered pipeline step.
     *
     * @param stage the stage the step belongs to
     * @param step the actual step implementation
     * @param priority the execution priority within the stage
     * @param asyncSafe whether the step may run off the main thread
     * @param registrationOrder the insertion order used as a tiebreaker
     */
    public record RegisteredPipelineStep(
            Stage stage,
            ChatPipelineStep step,
            int priority,
            boolean asyncSafe,
            long registrationOrder
    ) {
    }
}
