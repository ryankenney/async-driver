package info.ryankenney.jasync_driver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

public class JasyncDriverTest {

	private static class ExecuteLogEntry {
		Task<?,?> task;
		Object argument;
		ExecuteLogEntry(Task<?,?> task, Object argument) {
			this.task = task;
			this.argument = argument;
		}
	}

	/**
	 * <p>Verifies the basic function of {@link AsyncTask} within a
	 * {@link DriverBody} in three scenarios:</p>
	 * 
	 * <ul>
	 * <li>With arg and return</li>
	 * <li>With arg</li>
	 * <li>With return</li>
	 * </ul>
	 */
	@Test
	public void testAsyncTask() throws Exception {

		// Setup
		final ArrayList<Object> witnessedArguments = new ArrayList<>();
		final ArrayList<Object> witnessedReturns = new ArrayList<>();
		final AsyncTask<String, Integer> taskWithArgAndReturn = new AsyncTask<String, Integer>() {
			public void run(String arg, ResultHandler<Integer> resultHandler) {
				witnessedArguments.add(arg);
				resultHandler.reportComplete(1);
			}
		};
		final AsyncTask<Void, Integer> taskWithoutArg = new AsyncTask<Void, Integer>() {
			public void run(Void arg, ResultHandler<Integer> resultHandler) {
				witnessedArguments.add(arg);
				resultHandler.reportComplete(2);
			}
		};
		final AsyncTask<String, Void> taskWithoutReturn  = new AsyncTask<String, Void>() {
			public void run(String arg, ResultHandler<Void> resultHandler) {
				witnessedArguments.add(arg);
				resultHandler.reportComplete();
			}
		};
		
		// Execute and verify fails with exception
		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void  run() {
				witnessedReturns.clear();
				witnessedReturns.add(driver.execute(taskWithArgAndReturn, "arg1"));
				witnessedReturns.add(driver.execute(taskWithoutArg));
				witnessedReturns.add(driver.execute(taskWithoutReturn, "arg3"));
			};
		});
		
		// Verify
		assertEquals("arg1", (String) witnessedArguments.get(0));
		assertEquals((String) null, (String) witnessedArguments.get(1));
		assertEquals("arg3", (String) witnessedArguments.get(2));
		assertEquals(1, witnessedReturns.get(0));
		assertEquals(2, witnessedReturns.get(1));
		assertEquals(null, witnessedReturns.get(2));
	}

	/**
	 * <p>Verifies that if the path of execution through the {@link DriverBody}
	 * changes during the re-executions triggered by callbacks, the
	 * {@link JasyncDriver} detects the problem and throws an
	 * {@link UnstableConditionsException}.</p>
	 */
	@Test
	public void testUnstableConditionsException() throws Exception {

		final AtomicReference<String> externalResource= new AtomicReference<>("ready");

		final AsyncTask<Void, String> readExternalResource = new AsyncTask<Void, String>() {
			public void run(Void arg, ResultHandler<String> resultHandler) {
				resultHandler.reportComplete(externalResource.get());
			}
		};
		final AsyncTask<String, String> modifyExternalResource = new AsyncTask<String, String>() {
			public void run(String arg, ResultHandler<String> resultHandler) {
				externalResource.set("processed");
				resultHandler.reportComplete(null);
			}
		};
		final AsyncTask<String, String> handleFailure = new AsyncTask<String, String>() {
			public void run(String arg, ResultHandler<String> resultHandler) {
				resultHandler.reportComplete(null);
			}
		};
		
		// Execute and verify fails with exception
		final JasyncDriver driver = new JasyncDriver();
		try {
			driver.execute(new DriverBody() {
				public void  run() {
					String status = externalResource.get();
					if ("ready".equals(status)) {
						driver.execute(modifyExternalResource, null);
					}  else {
						driver.execute(handleFailure, null);
					}
				};
			});
			Assert.fail("Expected exception");
		} catch (UnstableConditionsException  e) {
			// Success
		}

		// Demonstrate that using the driver for all tasks resolves the problem
		final JasyncDriver driver2 = new JasyncDriver();
		driver2.execute(new DriverBody() {
			public void  run() {
				String status = driver2.execute(readExternalResource, null);
				if ("ready".equals(status)) {
					driver2.execute(modifyExternalResource, null);
				}  else {
					driver2.execute(handleFailure, null);
				}
			};
		});
	}

	/**
	 * <p>Executes {@link AsyncTask} and {@link SyncTask} in succession, verifying
	 * that:</p>
	 * 
	 * <ul>
	 * <li>Each is executed the correct number of times</li>
	 * <li>Arguments are correctly passed</li>
	 * </ul>
	 */
	@Test
	public void testChainingAsyncAndSyncTasks() throws Exception {

		// Setup tasks
		final ArrayList<ExecuteLogEntry> executedTasks = new ArrayList<>();
		final SyncTask<String, String> sync1  = new SyncTask<String, String>() {
			public String run(String arg) {
				executedTasks.add(new ExecuteLogEntry(this, arg));
				return  "from-sync-1";
			}
		};
		final SyncTask<String, String> sync2  = new SyncTask<String, String>() {
			public String run(String arg) {
				executedTasks.add(new ExecuteLogEntry(this, arg));
				return  "from-sync-2";
			}
		};
		final SyncTask<String, String> syncNotCalled = new SyncTask<String, String>() {
			public String run(String arg) {
				executedTasks.add(new ExecuteLogEntry(this, arg));
				return  "sync-not-called";
			}
		};
		final AsyncTask<String, String> async1 = new AsyncTask<String, String>() {
			public void run(String arg, ResultHandler<String> resultHandler) {
				executedTasks.add(new ExecuteLogEntry(this, arg));
				resultHandler.reportComplete("from-async-1");
			}
		};
		final AsyncTask<String, String> async2 = new AsyncTask<String, String>() {
			public void run(String arg, ResultHandler<String> resultHandler) {
				executedTasks.add(new ExecuteLogEntry(this, arg));
				resultHandler.reportComplete("from-async-2");
			}
		};
		final AsyncTask<String, String> asyncNotCalled = new AsyncTask<String, String>() {
			public void run(String arg, ResultHandler<String> resultHandler) {
				executedTasks.add(new ExecuteLogEntry(this, arg));
				resultHandler.reportComplete("async-not-called");
			}
		};

		// Execute
		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void  run() {
				String value = driver.execute(async1, "start");
				value = driver.execute(sync1, value);
				if (new AtomicBoolean(false).get()) {
					driver.execute(syncNotCalled, value);
				}
				if (new AtomicBoolean(false).get()) {
					driver.execute(asyncNotCalled, value);
				}
				value = driver.execute(async2, value);
				value = driver.execute(sync2, value);
			};
		});
		
		// Verify
		// ... Order of execution
		int i = 0;
		assertTrue(async1 == executedTasks.get(i++).task);
		assertTrue(sync1 == executedTasks.get(i++).task);
		assertTrue(async2 == executedTasks.get(i++).task);
		assertTrue(sync2 == executedTasks.get(i++).task);
		assertEquals(i, executedTasks.size());
		// ... Arguments
		i = 0;
		assertEquals("start", executedTasks.get(i++).argument);
		assertEquals("from-async-1", executedTasks.get(i++).argument);
		assertEquals("from-sync-1", executedTasks.get(i++).argument);
		assertEquals("from-async-2", executedTasks.get(i++).argument);
		assertEquals(i, executedTasks.size());
	}

	/**
	 * <p>Verifies that a task can be used multiple times in the same
	 * {@link DriverBody}, with different return values on each execution.</p>
	 */
	@Test
	public void testUsingTaskMultipleTimes() throws Exception {

		// Setup
		final AtomicInteger externalResource= new AtomicInteger(0);
		final AsyncTask<Void, Integer> incrementValue = new AsyncTask<Void, Integer>() {
			public void run(Void arg, ResultHandler<Integer> resultHandler) {
				resultHandler.reportComplete(externalResource.incrementAndGet());
			}
		};
		
		// Execute
		final AtomicInteger return1 = new AtomicInteger();
		final AtomicInteger return2 = new AtomicInteger();
		final AtomicInteger return3 = new AtomicInteger();
		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void  run() {
				return1.set(driver.execute( incrementValue, null));
				return2.set(driver.execute( incrementValue, null));
				return3.set(driver.execute( incrementValue, null));
			};
		});
		
		// Verify
		// ... correct return codes were recorded
		assertEquals(1, return1.get());
		assertEquals(2, return2.get());
		assertEquals(3, return3.get());
	}

	/**
	 * <p>
	 * Verifies that one {@link JasyncDriver} can execute another, wrapped in an
	 * {@link AsyncTask}, and that the resulting order of execution is correct.
	 * </p>
	 */
	@Test
	public void testNestingJasyncDriver() throws Exception {

		final ArrayList<String> taskExecutions = new ArrayList<>(); 
		
		// Setup
		final AsyncTask<Void, Integer> innerTask = new AsyncTask<Void, Integer>() {
			public void run(Void arg, ResultHandler<Integer> resultHandler) {
				taskExecutions.add("innerTask");
				resultHandler.reportComplete();
			}
		};
		final AsyncTask<Void, Integer> outerBeforeTask = new AsyncTask<Void, Integer>() {
			public void run(Void arg, ResultHandler<Integer> resultHandler) {
				taskExecutions.add("outerBeforeTask");
				resultHandler.reportComplete();
			}
		};
		final AsyncTask<Void, Integer> outerAfterTask = new AsyncTask<Void, Integer>() {
			public void run(Void arg, ResultHandler<Integer> resultHandler) {
				taskExecutions.add("outerAfterTask");
				resultHandler.reportComplete();
			}
		};
		final AsyncTask<Void, Integer> innerDriverWrapper = new AsyncTask<Void, Integer>() {
			public void run(Void arg, final ResultHandler<Integer> resultHandler) {
				final JasyncDriver driver = new JasyncDriver();
				driver.execute(new DriverBody() {
					public void  run() {
						driver.execute(innerTask);
						resultHandler.reportComplete();
					};
				});
			}
		};

		// Execute
		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void  run() {
				driver.execute(outerBeforeTask);
				driver.execute(innerDriverWrapper);
				driver.execute(outerAfterTask);
			};
		});
		
		// Verify
		// ... execution order of tasks
		assertEquals(3, taskExecutions.size());
		assertEquals("outerBeforeTask", taskExecutions.get(0));
		assertEquals("innerTask", taskExecutions.get(1));
		assertEquals("outerAfterTask", taskExecutions.get(2));
	}
}
