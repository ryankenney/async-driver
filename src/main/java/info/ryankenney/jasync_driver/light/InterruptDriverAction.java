package info.ryankenney.jasync_driver.light;


/**
 * A special {@AsyncAction} used to interrupt the entire body of the
 * {@link ActionDriver}.
 */
class InterruptDriverAction implements AsyncAction {

    private ActionDriver driver;
    
    public InterruptDriverAction(ActionDriver driver) {
        this.driver = driver;
    }

    public void run(final Runnable onComplete) {
        driver.doOnComplete();
    }
}
