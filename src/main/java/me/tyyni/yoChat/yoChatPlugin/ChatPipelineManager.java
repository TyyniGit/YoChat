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

    public void registerStep(Stage stage, @NonNull ChatPipelineStep step) {
        registerStep(stage, step, step.getPriority());
    }

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
            List<RegisteredPipelineStep> steps = pipelineSteps.get(stage);
            if (steps == null) continue;

            for (RegisteredPipelineStep step : steps) {
                executeStep(step, context);

                if (context.isCancelled()) return;
            }
        }
    }

    public List<RegisteredPipelineStep> getSteps(Stage stage) {
        List<RegisteredPipelineStep> steps = pipelineSteps.get(stage);
        return steps == null ? List.of() : List.copyOf(steps);
    }

    public Map<Stage, List<RegisteredPipelineStep>> getRegisteredSteps() {
        Map<Stage, List<RegisteredPipelineStep>> snapshot = new ConcurrentHashMap<>();
        for (Stage stage : Stage.values()) {
            snapshot.put(stage, getSteps(stage));
        }
        return snapshot;
    }

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

    public record RegisteredPipelineStep(
            Stage stage,
            ChatPipelineStep step,
            int priority,
            boolean asyncSafe,
            long registrationOrder
    ) {
    }
}
