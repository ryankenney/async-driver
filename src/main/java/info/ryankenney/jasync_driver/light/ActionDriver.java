package info.ryankenney.jasync_driver.light;


/**
 * <p>
 * Executes an {@link AsyncAction}, which can be constructed from any number of
 * other {@link AsyncAction}s. The significance of this class is that it:
 * </p>
 * 
 * <ul>
 * <li>Provides a lot of helper methods for constructing if/then/series logic.</li>
 * <li>Represents the scope of interruption if the process hits an
 * {@InterruptDriverAction} (e.g. the logic includes a
 * call to {@link #stop()}.</li>
 * </ul>
 * 
 * <p>
 * Note that this class can be used as a generic {@link Runnable} or as an
 * {@link AsyncAction} within the body of another {@link ActionDriver}.
 * </p>
 */
public class ActionDriver implements AsyncAction, Runnable {

    private AsyncAction body;
    private Runnable onComplete;

    /**
     * Sets the body of this driver. Should be called once, and before
     * {@link #run()} or {@link #run(Runnable)} is called.
     * 
     * @param action
     * @return
     */
    public ActionDriver setBody(AsyncAction action) {
        this.body = action;
        return this;
    }

    /**
     * Creates a an {@link AsyncAction} that executes a series of
     * {@link AsyncAction} in serial.
     * 
     * @param actions
     * @return
     */
    public AsyncAction series(AsyncAction... actions) {
        return new ActionSeries(actions);
    }

    /**
     * Creates a an {@link AsyncAction} that will stop the entire body of this
     * {@link ActionDriver}. Note that if this {@link ActionDriver} was launched
     * with {@link #run(Runnable)}, and a callback was provided, that callback
     * will still be run. In other words, this method short-circuits all actions
     * in the body of the driver, but it doesn't prevent the execution of the
     * callback provided by it's caller.
     */
    public AsyncAction stop() {
        return new InterruptDriverAction(this);
    }

    /**
     * Returns an {@link AsyncAction} that will execute the provided
     * {@link AsyncAction} if the provided reference points to a value of true.
     * The provided condition reference should never point to a value of null at
     * the time this action is executed.
     */
    public AsyncAction doIf(Ref<Boolean> condition, AsyncAction onTrue) {
        return new IfAction(condition, onTrue, null);
    }

    /**
     * Returns an {@link AsyncAction} that will execute the provided
     * {@link AsyncAction} if the provided reference points to a value of false.
     * The provided condition reference should never point to a value of null at
     * the time this action is executed.
     */
    public AsyncAction doIfNot(Ref<Boolean> condition, AsyncAction onFalse) {
        return new IfAction(condition, null, onFalse);
    }

    /**
     * Returns an {@link AsyncAction} that will execute either the of the
     * provided {@link AsyncAction}s, depending upon the true/false value of the
     * provided condition reference. The provided condition reference should
     * never point to a value of null at the time this action is executed.
     * Either of the provided {@link AsyncAction}s may be null.
     */
    public AsyncAction doIfElse(Ref<Boolean> condition, AsyncAction onTrue, AsyncAction onFalse) {
        return new IfAction(condition, onTrue, onFalse);
    }

    /**
     * Returns an {@link AsyncAction} that simply wraps a {@link SyncAction}, so
     * it can be used with the other features of this library.
     */
    public AsyncAction exec(final SyncAction action) {
        return new AsyncAction() {
            @Override
            public void run(Runnable onComplete) {
                action.run();
                RunUtil.I.runIfDefined(onComplete);
            }
        };
    }

    /**
     * Simply returns the provided {@link AsyncAction}. This method is analogous
     * to {@link #exec(SyncAction)}, and is included to allow for more symmetric
     * looking code.
     */
    public AsyncAction exec(final AsyncAction action) {
        return action;
    }

    /**
     * Executes the body of the driver, with no overall "on complete" callback.
     */
    @Override
    public void run() {
        this.run(null);
    }

    /**
     * Executes the body of the driver, applying the provided callback at the
     * completion.
     */
    @Override
    public void run(Runnable onComplete) {
        if (onComplete == null) {
            onComplete = new NoOpRunnable();
        }
        setOnComplete(onComplete);
        body.run(onComplete);
    }

    /**
     * Executes any overall "on complete" callback that was registered with this
     * driver
     */
    void doOnComplete() {
        RunUtil.I.runIfDefined(onComplete);
    }

    private void setOnComplete(Runnable onComplete) {
        if (this.onComplete != null) {
            throw new RuntimeException("onComplete set multiple times. Was the class reused?");
        }
        this.onComplete = onComplete;
    }
}
