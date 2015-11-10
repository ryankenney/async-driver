package info.ryankenney.jasync_driver.example;

import info.ryankenney.jasync_driver.example.supporting.UserInterface;
import info.ryankenney.jasync_driver.example.supporting.WebServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Provides demonstrations of the library via junit {@link Test} methods.
 * 
 * @author rkenney
 */
public class Demos {

	/**
	 * Demonstrates {@link ExampleApp}.
	 */
	@Test
	public void demo() throws Exception {
		// Create browser thread task to terminate it upon completion
		final ExecutorService browserThread = Executors.newFixedThreadPool(1);
		Runnable onComplete = new Runnable() {
			public void run() {
				browserThread.shutdown();
			}
		};
		
		// Execute
		new ExampleApp(new WebServer(browserThread), new UserInterface(browserThread))
			.onUserClick(onComplete);
		
		// Wait for thread to terminate
		browserThread.awaitTermination(2, TimeUnit.MINUTES);
	}

	/**
	 * Demonstrates {@link ExampleAppWithLogging}.
	 */
	@Test
	public void demoWithLogging() throws Exception {
		// Create browser thread task to terminate it upon completion
		final ExecutorService browserThread = Executors.newFixedThreadPool(1);
		Runnable onComplete = new Runnable() {
			public void run() {
				browserThread.shutdown();
			}
		};
		
		// Execute
		new ExampleAppWithLogging(new WebServer(browserThread), new UserInterface(browserThread))
			.onUserClick(onComplete);
		
		// Wait for thread to terminate
		browserThread.awaitTermination(2, TimeUnit.MINUTES);
	}
}
