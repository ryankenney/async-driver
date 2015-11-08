package info.ryankenney.jasync_driver;

/**
 * An {@link Error} used to interrupt execution of the {@link JasyncDriver}
 * immediately after an asynchronous action is initiated (and the
 * {@link JasyncDriver} is awaiting for the asynchronous callback to wake it
 * back up).
 * 
 * @author rkenney
 */
@SuppressWarnings("serial")
public class JasyncActionSubmittedInterrupt extends Error {

}
