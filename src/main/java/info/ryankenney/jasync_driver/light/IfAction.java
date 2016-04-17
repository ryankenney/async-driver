package info.ryankenney.jasync_driver.light;


public class IfAction implements AsyncAction {

    private Ref<Boolean> condition;
    private AsyncAction trueAction;
    private AsyncAction falseAction;

    public IfAction(Ref<Boolean> condition, AsyncAction trueAction, AsyncAction falseAction) {
        this.condition = condition;
        this.trueAction = trueAction;
        this.falseAction = falseAction;
    }

    @Override
    public void run(Runnable onComplete) {
        boolean conditionTrue = condition.get(); 
        if (conditionTrue && trueAction != null) {
            trueAction.run(onComplete);
        } else if (!conditionTrue && falseAction != null) {
            falseAction.run(onComplete);
        } else {
            RunUtil.I.runIfDefined(onComplete);
        }
    }
}
