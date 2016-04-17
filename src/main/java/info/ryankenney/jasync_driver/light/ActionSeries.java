package info.ryankenney.jasync_driver.light;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * Contains a series of 
 * @author rkenney
 *
 */
public class ActionSeries implements AsyncAction {

    private LinkedList<AsyncAction> remainingActions = new LinkedList<>();
    
    public ActionSeries(AsyncAction... actions) {
        remainingActions.addAll(Arrays.asList(actions));
    }
    
    @Override
    public void run(final Runnable onComplete) {
        if (remainingActions.size() < 1) {
            RunUtil.I.runIfDefined(onComplete);
            return;
        }
        remainingActions.removeFirst().run(new Runnable() {
            @Override
            public void run() {
                ActionSeries.this.run(onComplete);
            }
        });
    }
}
