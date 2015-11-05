package info.ryankenney.async_driver;

public interface SyncTask<A,R> extends Task<A,R> {

	R run(A arg);
	
}
