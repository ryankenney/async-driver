package info.ryankenney.async_driver;

public interface ResultHandler<R> {
	
	void reportComplete(R result);

	void reportComplete();

}
