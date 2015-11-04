package info.ryankenney.async_driver;

import static org.junit.Assert.assertEquals;
import info.ryankenney.async_driver.util.Ref;

import org.junit.Test;

public class AsyncDriverTest {

	@Test
	public void test() throws Exception {

		final Ref<Integer> lookupNameCount = new Ref<>(0); 
		final Ref<Integer> updateScoreCount = new Ref<>(0); 
		
		final AsyncDriver driver = new AsyncDriver();
		final AsyncTask<Void, String> lookupName = new AsyncTask<Void, String>() {
			public void run(Void arg, ResultHandler<String> resultHandler) {
				lookupNameCount.set(lookupNameCount .get()+1);
				resultHandler.reportComplete("Name");
			}
		};
		final AsyncTask<String, Void> updateScore = new AsyncTask<String, Void>() {
			public void run(String arg, ResultHandler<Void> resultHandler) {
				updateScoreCount.set(updateScoreCount.get()+1);
				resultHandler.reportComplete(null);
			}
		};
		final SyncTask<String, Boolean> nameIsSelected = new SyncTask<String, Boolean>() {
			@Override
			public Boolean run(String arg) {
				return true;
			}
		};
		driver.execute(new DriverBody() {
			public void  run() {
				String name = driver.execute(lookupName, null);
				driver.execute(updateScore, name);
				if (driver.execute(nameIsSelected, name)) {
					
				}
			};
		});
		
		assertEquals((Integer)1, lookupNameCount.get());
		assertEquals((Integer)1, updateScoreCount.get());
		
	}

	// TODO [rkenney]: Remove if not used
	static class MyDatabase {
		public static void loadData(Handler<String> resultHandler) {
			// Simulated
			resultHandler.handle("x");
		}
		public static void storeData(String data, Handler<Void> actionCompleteHandler) {
			// Simulated
			actionCompleteHandler.handle(null);
		}
	}
	
	static interface Handler<V> {
		public void handle(V value);
	}
	
}
