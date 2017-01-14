package org.gradle.profiler;

import joptsimple.OptionSet;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class CompositeProfiler extends Profiler {
    private final List<Profiler> delegates;
    private final Map<Profiler, Object> profilerOptions = new HashMap<>();

    CompositeProfiler(final List<Profiler> delegates) {
        this.delegates = delegates;
    }

    @Override
    public String toString() {
        return delegates.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    @Override
    public ProfilerController newController(final String pid, final ScenarioSettings settings) {
        List<ProfilerController> controllers = delegates.stream()
                .map((Profiler prof) -> prof.newController(pid,
                        settingsFor(prof, settings)))
                .collect(Collectors.toList());
        return new ProfilerController() {
            @Override
            public void start() throws IOException, InterruptedException {
                for (ProfilerController controller : controllers) {
                    controller.start();
                }
            }

            @Override
            public void stop() throws IOException, InterruptedException {
                for (ProfilerController controller : controllers) {
                    controller.stop();
                }
            }
        };
    }

    private ScenarioSettings settingsFor(final Profiler prof, final ScenarioSettings scenarioSettings) {
        InvocationSettings settings = scenarioSettings.getInvocationSettings();
        InvocationSettings newSettings = new InvocationSettings(settings.getProjectDir(), prof, profilerOptions.get(prof), settings.isBenchmark(),
                settings.getOutputDir(), settings.getInvoker(), settings.isDryRun(), settings.getScenarioFile(), settings.getVersions(),
                settings.getTargets(), settings.getSystemProperties(), settings.getGradleUserHome(), settings.getWarmUpCount(),
                settings.getBuildCount());
        return new ScenarioSettings(newSettings, scenarioSettings.getScenario());
    }

    @Override
    public JvmArgsCalculator newJvmArgsCalculator(final ScenarioSettings settings) {
        return new JvmArgsCalculator() {
            @Override
            public void calculateJvmArgs(final List<String> jvmArgs) {
                delegates.forEach(prof -> prof.newJvmArgsCalculator(settingsFor(prof, settings)).calculateJvmArgs(jvmArgs));
            }
        };
    }

    @Override
    public JvmArgsCalculator newInstrumentedBuildsJvmArgsCalculator(ScenarioSettings settings) {
        return new JvmArgsCalculator() {
            @Override
            public void calculateJvmArgs(final List<String> jvmArgs) {
                delegates.forEach(prof -> prof.newInstrumentedBuildsJvmArgsCalculator(settingsFor(prof, settings)).calculateJvmArgs(jvmArgs));
            }
        };
    }

    @Override
    public GradleArgsCalculator newGradleArgsCalculator(ScenarioSettings settings) {
        return new GradleArgsCalculator(){
            @Override
            public void calculateGradleArgs(List<String> gradleArgs) {
                delegates.forEach(prof -> prof.newGradleArgsCalculator(settingsFor(prof, settings)).calculateGradleArgs(gradleArgs));
            }
        };
    }

    @Override
    public GradleArgsCalculator newInstrumentedBuildsGradleArgsCalculator(ScenarioSettings settings) {
        return new GradleArgsCalculator(){
            @Override
            public void calculateGradleArgs(List<String> gradleArgs) {
                delegates.forEach(prof -> prof.newInstrumentedBuildsGradleArgsCalculator(settingsFor(prof, settings)).calculateGradleArgs(gradleArgs));
            }
        };
    }

    @Override
    public Object newConfigObject(final OptionSet parsedOptions) {
        for (Profiler delegate : delegates) {
            profilerOptions.put(delegate, delegate.newConfigObject(parsedOptions));
        }
        return Collections.unmodifiableMap(profilerOptions);
    }
}
