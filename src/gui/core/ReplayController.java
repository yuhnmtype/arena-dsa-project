package gui.core;

import engine.BattleLog;
import engine.BattleLogEntry;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

/**
 * ReplayController — steps through the BattleLog by TICK GROUP.
 *
 * In the simultaneous engine each round is logged in two tick groups:
 *   tick=0  all MOVE actions (applied at once)
 *   tick=1  all ATTACK/SKILL actions (applied at once)
 *
 * One call to next() / prev() advances or retreats by one full tick group,
 * so the board updates to show all units acting simultaneously rather than
 * one champion at a time.
 *
 * groupEnds[i] = the exclusive end-index of tick group i in the log.
 * currentGroup = -1 means "initial board, no entries applied".
 */
public class ReplayController {

    private final BattleLog log;

    // Tick-group boundaries pre-computed at construction.
    private final List<Integer> groupEnds = new ArrayList<>();
    private int currentGroup = -1;  // -1 = initial board

    private Timer autoTimer;
    private int autoDelayMs = 800;

    private final List<StepListener> listeners = new ArrayList<>();

    public interface StepListener {
        void onStepChanged(BattleLogEntry entry, int step, int total);
    }

    public ReplayController(BattleLog log) {
        this.log = log;
        buildGroups();
        this.autoTimer = new Timer(autoDelayMs, e -> next());
    }

    // ── Precompute tick-group end indices ─────────────────────────
    private void buildGroups() {
        if (log.totalSteps() == 0) return;
        BattleLogEntry prev = null;
        for (int i = 0; i < log.totalSteps(); i++) {
            BattleLogEntry e = log.getEntry(i);
            if (prev != null && (e.round != prev.round || e.tick != prev.tick)) {
                groupEnds.add(i);   // end of previous group
            }
            prev = e;
        }
        groupEnds.add(log.totalSteps()); // end of last group
    }

    // ── currentStep = how many log entries have been applied ──────
    private int entryCount() {
        if (currentGroup < 0) return 0;
        return groupEnds.get(currentGroup);
    }

    // ── Navigation ────────────────────────────────────────────────
    public void next() {
        if (currentGroup + 1 < groupEnds.size()) {
            currentGroup++;
            notifyListeners();
        } else {
            pause();
        }
    }

    public void prev() {
        if (currentGroup >= 0) {
            currentGroup--;
            notifyListeners();
        }
    }

    public void reset() {
        pause();
        currentGroup = -1;
        notifyListeners();
    }

    public void auto()  { autoTimer.setDelay(autoDelayMs); autoTimer.start(); }
    public void pause() { autoTimer.stop(); }

    public void setSpeed(int delayMs) {
        this.autoDelayMs = delayMs;
        autoTimer.setDelay(delayMs);
    }

    // ── Notify all panels ─────────────────────────────────────────
    private void notifyListeners() {
        int step = entryCount();
        // Pass the last entry of the current group (or null for initial board)
        BattleLogEntry entry = (step > 0) ? log.getEntry(step - 1) : null;
        for (StepListener l : listeners)
            l.onStepChanged(entry, step, log.totalSteps());
    }

    public void addListener(StepListener listener) {
        listeners.add(listener);
    }

    // ── Accessors ─────────────────────────────────────────────────
    public int getCurrentStep()  { return entryCount(); }
    public int getTotalSteps()   { return log.totalSteps(); }
    public int getTotalGroups()  { return groupEnds.size(); }
    public int getCurrentGroup() { return currentGroup; }
    public BattleLog getLog()    { return log; }
    public boolean isRunning()   { return autoTimer.isRunning(); }

    public BattleLogEntry getCurrentEntry() {
        int step = entryCount();
        return step > 0 ? log.getEntry(step - 1) : null;
    }

    public int getCurrentRound() {
        BattleLogEntry e = getCurrentEntry();
        return e != null ? e.round : 0;
    }
}