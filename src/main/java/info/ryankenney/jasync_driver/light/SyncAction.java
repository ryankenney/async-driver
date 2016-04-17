package info.ryankenney.jasync_driver.light;



/**
 * <p>
 * A simpler form of {@link AsyncAction} that can be used for synchronous
 * actions, which don't need to execute an "on complete" callback.
 * </p>
 * 
 * <p>
 * In order to use these with the other constructs of the {@link ActionDriver},
 * you can conver this to an {@link AsyncAction} by calling
 * {@link ActionDriver#exec(AsyncAction)}.
 * </p>
 */
public interface SyncAction {
    /**
     * The body of the action.
     */
    public void run();
}
