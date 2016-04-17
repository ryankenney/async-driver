package info.ryankenney.jasync_driver.light;


/**
 * Represents a single asynchronous action that is to be chained together with
 * other {@link AsyncAction}s and/or {@link SyncAction}s in the body of an
 * {@link ActionDriver}.
 */
public interface AsyncAction {

    /**
     * The body of the action.
     * 
     * @param onComplete
     *            Called when the action is complete. This will never be null.
     */
    public void run(Runnable onComplete);
}
