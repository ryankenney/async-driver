package info.ryankenney.jasync_driver.example;

import java.util.ArrayList;
import java.util.List;

public class ExampleBadApp {
	
	static interface User {
		public String getName();
	}
	
	static class Permissions {
		public List<String> permissions = new ArrayList<>();
	}

	static interface WebServer {
		public void readUserPermissions(User  user, ReturnCallback<Permissions> permission);

		public void storeValue(String value,  ReturnCallback<Status> returnCallback);
	}

	static interface UserInterface {
		public void showError(String message);
		public void promptForNewValue(ReturnCallback<String> returnCallback);
	}

	static interface ReturnCallback<R> {
		public void handleResult(R result);
	}
	
	static enum Status {
		OK,
		FAILURE
	}
	
	WebServer webServer;
	UserInterface userInterface;
	User user;
	
	public void onUserClick() {
		readUserPermissions();
	}

	void readUserPermissions() {
		webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
			public void handleResult(Permissions permissions) {
				if (permissions.permissions.contains("edit")) {
					promptUserForNewValue();
				} else {
					notifyPermissionsError();
				}
			}
		});
	}

	void notifyPermissionsError() {
		userInterface.showError("User does not have edit permission");
	}

	void promptUserForNewValue() {
		userInterface.promptForNewValue(new ReturnCallback<String> () {
			@Override
			public void handleResult(String result) {
				updateStoredValue(result);
			}
		});
	}

	void updateStoredValue(String value) {
		webServer.storeValue(value, new ReturnCallback<Status> () {
			@Override
			public void handleResult(Status result) {
				if (result != Status.OK) {
					notifyStoreError();
				}
			}
		});
	}

	void notifyStoreError() {
		userInterface.showError("Store action failed");
	}

}
