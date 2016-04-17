package info.ryankenney.jasync_driver.light;


public class RunUtil {

    public static final RunUtil I = new RunUtil();
    
    /**
     * Executes the {@link Runnable} iff it's non-null.
     */
    public void runIfDefined(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        runnable.run();
    }

}
