package info.ryankenney.jasync_driver;

public interface SyncTask<A,R> extends Task<A,R> {

	R run(A arg);
	
}
