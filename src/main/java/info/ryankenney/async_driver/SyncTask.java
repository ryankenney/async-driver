package info.ryankenney.async_driver;

public interface SyncTask<A,R> extends Task {

	R run(A arg);
	
}
