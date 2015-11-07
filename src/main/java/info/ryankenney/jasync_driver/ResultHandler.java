package info.ryankenney.jasync_driver;

public interface ResultHandler<R> {
	
	void reportComplete(R result);

	void reportComplete();

}
