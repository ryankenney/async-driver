package info.ryankenney.async_driver.example.supporting;

public interface ReturnCallback<R> {
	void handleResult(R result);
}
