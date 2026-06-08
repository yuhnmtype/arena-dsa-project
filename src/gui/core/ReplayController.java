package gui.core;

import engine.BattleLog;
import engine.BattleLogEntry;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

public class ReplayController {

    private final BattleLog log;
    private int currentStep = 0;
    private Timer autoTimer;
    private int autoDelayMs = 800;

    // Listeners để notify panels khi step thay đổi
    private final List<StepListener> listeners = new ArrayList<>();

    public interface StepListener {
        void onStepChanged(BattleLogEntry entry, int step, int total);
    }

    public ReplayController(BattleLog log) {
        this.log = log;
        this.autoTimer = new Timer(autoDelayMs, e -> next());
    }

    public void addListener(StepListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        BattleLogEntry entry = log.getEntry(currentStep);
        for (StepListener l : listeners)
            l.onStepChanged(entry, currentStep, log.totalSteps());
    }

    public void next() {
        if (currentStep < log.totalSteps() - 1) {
            currentStep++;
            notifyListeners();
        } else {
            pause(); // stop at end
        }
    }

    public void prev() {
        if (currentStep > 0) {
            currentStep--;
            notifyListeners();
        }
    }

    public void reset() {
        pause();
        currentStep = 0;
        notifyListeners();
    }

    public void auto() {
        autoTimer.setDelay(autoDelayMs);
        autoTimer.start();
    }

    public void pause() {
        autoTimer.stop();
    }

    public void setSpeed(int delayMs) {
        this.autoDelayMs = delayMs;
        autoTimer.setDelay(delayMs);
    }

    public boolean isRunning() {
        return autoTimer.isRunning();
    }

    public int getCurrentStep()  { return currentStep; }
    public int getTotalSteps()   { return log.totalSteps(); }
    public BattleLog getLog()    { return log; }

    public BattleLogEntry getCurrentEntry() {
        return log.getEntry(currentStep);
    }

    public int getCurrentRound() {
        BattleLogEntry e = getCurrentEntry();
        return e != null ? e.round : 0;
    }
}