package info.ryankenney.jasync_driver.light;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class ActionDriverTest {

    /**
     * Verifies that {@link ActionDriver#run()} works in the simplest case
     */
    @Test
    public void testRun() throws Exception {
        // Setup
        final AtomicInteger callCount = new AtomicInteger(0);

        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(new AsyncIncrementAction(callCount));
        driver.run();

        // Verify
        assertEquals(1, callCount.get());
    }

    /**
     * Verifies that {@link ActionDriver#run(Runnable)} executes the body and
     * the provided "on complete" action
     */
    @Test
    public void testRunAction() throws Exception {
        // Setup
        final AtomicInteger callCount = new AtomicInteger(0);
        final AtomicInteger followOnCount = new AtomicInteger(0);

        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(new AsyncIncrementAction(callCount));
        driver.run(new IncrementRunnable(followOnCount));

        // Verify
        assertEquals(1, callCount.get());
        assertEquals(1, followOnCount.get());
    }

    /**
     * Verifies that all actions in an {@link ActionSeries} (created via
     * {@link ActionDriver#series(AsyncAction...)}) are executed
     */
    @Test
    public void testSeries() throws Exception {
        // Setup
        final AtomicInteger callCountA = new AtomicInteger(0);
        final AtomicInteger callCountB = new AtomicInteger(0);
        final AtomicInteger callCountC = new AtomicInteger(0);

        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(driver.series(
                new AsyncIncrementAction(callCountA),
                new AsyncIncrementAction(callCountB),
                new AsyncIncrementAction(callCountC)));
        driver.run();

        // Verify
        assertEquals(1, callCountA.get());
        assertEquals(1, callCountB.get());
        assertEquals(1, callCountC.get());
    }
    
    /**
     * Verifies that actions can be reused within an {@link ActionDriver} body
     * and an {@link ActionSeries}
     */
    @Test
    public void testActionReuse() throws Exception {
        // Setup
        final AtomicInteger callCountA = new AtomicInteger(0);
        final AtomicInteger callCountB = new AtomicInteger(0);
        final AtomicInteger callCountC = new AtomicInteger(0);
        final AsyncIncrementAction actionA = new AsyncIncrementAction(callCountA);
        final AsyncIncrementAction actionB = new AsyncIncrementAction(callCountB);
        final AsyncIncrementAction actionC = new AsyncIncrementAction(callCountC);
        
        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(driver.series(
                actionA,
                actionB,
                actionA,
                actionC,
                actionC,
                actionC));
        driver.run();

        // Verify
        assertEquals(2, callCountA.get());
        assertEquals(1, callCountB.get());
        assertEquals(3, callCountC.get());
    }
    
    /**
     * Verifies that {@link AsyncAction}s and {@link SyncAction}s can be mixed
     * within an {@link ActionDriver} body and an {@link ActionSeries}
     */
    @Test
    public void testAsyncAndSyncActions() throws Exception {
        // Setup
        final AtomicInteger syncCount = new AtomicInteger(0);
        final AtomicInteger asyncCount = new AtomicInteger(0);
        final SyncIncrementAction syncAction = new SyncIncrementAction(syncCount);
        final AsyncIncrementAction asyncAction = new AsyncIncrementAction(asyncCount);
        
        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.series(
                        driver.exec(syncAction),
                        driver.exec(asyncAction),
                        driver.exec(syncAction),
                        driver.exec(asyncAction),
                        driver.exec(asyncAction),
                        driver.exec(syncAction),
                        driver.exec(syncAction)));
        driver.run();

        // Verify
        assertEquals(4, syncCount.get());
        assertEquals(3, asyncCount.get());
    }

    /**
     * Verifies that {@link ActionDriver#doIf(Ref, AsyncAction)} executes the
     * action if the condition is true
     */
    @Test
    public void testDoIf_True() throws Exception {
        // Setup
        final AtomicInteger callCount = new AtomicInteger(0);
        Ref<Boolean> condition = new Ref<>(true);
        
        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.doIf(condition, 
                        driver.exec(new AsyncIncrementAction(callCount))));
        driver.run();

        // Verify
        assertEquals(1, callCount.get());
    }

    /**
     * Verifies that {@link ActionDriver#doIf(Ref, AsyncAction)} does not
     * execute the action if the condition is false
     */
    @Test
    public void testDoIf_False() throws Exception {
        // Setup
        final AtomicInteger callCount = new AtomicInteger(0);
        Ref<Boolean> condition = new Ref<>(false);
        
        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.doIf(condition, 
                        driver.exec(new AsyncIncrementAction(callCount))));
        driver.run();

        // Verify
        assertEquals(0, callCount.get());
    }

    /**
     * Verifies that {@link ActionDriver#doIfNot(Ref, AsyncAction)} does not
     * execute the action if the condition is true
     */
    @Test
    public void testDoIfNot_True() throws Exception {
        // Setup
        final AtomicInteger callCount = new AtomicInteger(0);
        Ref<Boolean> condition = new Ref<>(true);
        
        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.doIfNot(condition, 
                        driver.exec(new AsyncIncrementAction(callCount))));
        driver.run();

        // Verify
        assertEquals(0, callCount.get());
    }

    /**
     * Verifies that {@link ActionDriver#doIfNot(Ref, AsyncAction)} executes the
     * action if the condition is false
     */
    @Test
    public void testDoIfNot_False() throws Exception {
        // Setup
        final AtomicInteger callCount = new AtomicInteger(0);
        Ref<Boolean> condition = new Ref<>(false);
        
        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.doIfNot(condition, 
                        driver.exec(new AsyncIncrementAction(callCount))));
        driver.run();

        // Verify
        assertEquals(1, callCount.get());
    }

    /**
     * Verifies that {@link ActionDriver#doIfElse(Ref, AsyncAction)} only
     * executes the true action if the condition is true
     */
    @Test
    public void testDoIfElse_True() throws Exception {
        // Setup
        final AtomicInteger callCountTrue = new AtomicInteger(0);
        final AtomicInteger callCountFalse = new AtomicInteger(0);
        Ref<Boolean> condition = new Ref<>(true);
        
        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.doIfElse(condition, 
                        driver.exec(new AsyncIncrementAction(callCountTrue)),
                        driver.exec(new AsyncIncrementAction(callCountFalse))));
        driver.run();

        // Verify
        assertEquals(1, callCountTrue.get());
        assertEquals(0, callCountFalse.get());
    }

    /**
     * Verifies that {@link ActionDriver#doIfElse(Ref, AsyncAction)} only
     * executes the true action if the condition is true
     */
    @Test
    public void testDoIfElse_False() throws Exception {
        // Setup
        final AtomicInteger callCountTrue = new AtomicInteger(0);
        final AtomicInteger callCountFalse = new AtomicInteger(0);
        Ref<Boolean> condition = new Ref<>(false);
        
        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.doIfElse(condition, 
                        driver.exec(new AsyncIncrementAction(callCountTrue)),
                        driver.exec(new AsyncIncrementAction(callCountFalse))));
        driver.run();

        // Verify
        assertEquals(0, callCountTrue.get());
        assertEquals(1, callCountFalse.get());
    }

    /**
     * Verifies that a {@link ActionDriver#doIf(Ref, AsyncAction)} inside of a
     * {@link ActionDriver#series(AsyncAction...)} does not interrupt the flow
     * of the series, regarless of true/false evaluation.
     */
    @Test
    public void testIfThen_ResumesSeries() throws Exception {
        // Setup
        final AtomicInteger callCountA = new AtomicInteger(0);
        final AtomicInteger callCountB = new AtomicInteger(0);
        final AtomicInteger callCountC = new AtomicInteger(0);
        final AtomicInteger callCountD = new AtomicInteger(0);
        final AtomicInteger callCountE = new AtomicInteger(0);
        final AsyncIncrementAction actionA = new AsyncIncrementAction(callCountA);
        final AsyncIncrementAction actionB = new AsyncIncrementAction(callCountB);
        final AsyncIncrementAction actionC = new AsyncIncrementAction(callCountC);
        final AsyncIncrementAction actionD = new AsyncIncrementAction(callCountD);
        final AsyncIncrementAction actionE = new AsyncIncrementAction(callCountE);
        Ref<Boolean> trueCondition = new Ref<>(true);
        Ref<Boolean> falseCondition = new Ref<>(false);

        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.series(
                        actionA,
                        driver.doIf(trueCondition, 
                                actionB),
                        actionC,
                        driver.doIf(falseCondition, 
                                actionD),
                        actionE));
        driver.run();

        // Verify
        assertEquals(1, callCountA.get());
        assertEquals(1, callCountB.get());
        assertEquals(1, callCountC.get());
        assertEquals(0, callCountD.get());
        assertEquals(1, callCountE.get());
    }


    /**
     * Verifies that {@link ActionDriver#stop()} interrupts all actions in the
     * body of the driver, but still executes the overall callback.
     */
    @Test
    public void testStop() throws Exception {
        // Setup
        final AtomicInteger callCountA = new AtomicInteger(0);
        final AtomicInteger callCountB = new AtomicInteger(0);
        final AtomicInteger callCountC = new AtomicInteger(0);
        final AtomicInteger callCountD = new AtomicInteger(0);
        final AtomicInteger callCountE = new AtomicInteger(0);
        final AtomicInteger followOnCount = new AtomicInteger(0);
        final AsyncIncrementAction actionA = new AsyncIncrementAction(callCountA);
        final AsyncIncrementAction actionB = new AsyncIncrementAction(callCountB);
        final AsyncIncrementAction actionC = new AsyncIncrementAction(callCountC);
        final AsyncIncrementAction actionD = new AsyncIncrementAction(callCountD);
        final AsyncIncrementAction actionE = new AsyncIncrementAction(callCountE);
        final IncrementRunnable followOnAction = new IncrementRunnable(followOnCount);
        Ref<Boolean> condition = new Ref<>(false);

        // Execute
        ActionDriver driver = new ActionDriver();
        driver.setBody(
                driver.series(
                        actionA,
                        actionB,
                        driver.doIfElse(condition, 
                                actionC,
                                driver.stop()),
                        actionD,
                        actionE));
        driver.run(followOnAction);

        // Verify
        assertEquals(1, callCountA.get());
        assertEquals(1, callCountB.get());
        assertEquals(0, callCountC.get());
        assertEquals(0, callCountD.get());
        assertEquals(0, callCountE.get());
        assertEquals(1, followOnCount.get());
    }

    private static class AsyncIncrementAction implements AsyncAction {
        private AtomicInteger counter;
        AsyncIncrementAction(AtomicInteger counter) {
            this.counter = counter;
        }
        @Override
        public void run(Runnable onComplete) {
            counter.incrementAndGet();
            onComplete.run();
        }
    }

    private static class SyncIncrementAction implements SyncAction {
        private AtomicInteger counter;
        SyncIncrementAction(AtomicInteger counter) {
            this.counter = counter;
        }
        @Override
        public void run() {
            counter.incrementAndGet();
        }
    }

    private static class IncrementRunnable implements Runnable {
        private AtomicInteger counter;
        IncrementRunnable(AtomicInteger counter) {
            this.counter = counter;
        }
        @Override
        public void run() {
            counter.incrementAndGet();
        }
    }
}
