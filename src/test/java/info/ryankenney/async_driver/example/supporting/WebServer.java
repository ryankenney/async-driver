package info.ryankenney.async_driver.example.supporting;

public interface WebServer {
	void readUserPermissions(User  user, ReturnCallback<Permissions> permission);
	void storeValue(String value,  ReturnCallback<Status> returnCallback);
}
