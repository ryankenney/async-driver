async-driver
====================

The Problem
--------------------

Imagine that you want to model this logic in a gui:

* On User Click
	* Read User Permissions
	* If User Has "edit" Permissions
		* Prompt User for New Value
		* Store New Value
		* If Store Fails
			* Notify User of Failure
	* If User Does Not Have "edit" Permissions
		* Notify User of Permissions Issue

Because many of these actions are "asynchronous" (they require a server or user response before resuming), you can end up with code that looks like this:

```java
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
```

Where did that nice little block of conditional logic go? It got smeared across all of the callback methods necesssary to string the asynchronous actions together.

A New Solution (async-driver)
--------------------

With async-driver, we can define the logic block as if everything is synchronous.
For example, this models the logic above:

```java
	final AsyncDriver driver = new AsyncDriver();
	driver.execute(new DriverBody() {
		public void run() {
			
			Permissions permissions = driver.execute(readUserPermissions, user);
			if (driver.execute(hasEditPermission, permissions)) {
				driver.execute(notifyPermissionsError);
			} else {
				String userInput = driver.execute(promptUserForNewValue);
				Status storeStatus = driver.execute(updateStoredValue, userInput);
				if (storeStatus != Status.OK) {
					driver.execute(notifyStoreError);
				}
			}
		}
	});
```

From here, you have to wrap each of the asynchronous actions in Task object,
but the resulting code is arguably much more legible. Here is a full example:

```java
WebServer webServer;
UserInterface userInterface;
User user;

public void onUserClick() {
	
	final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
		public void run(final User user, final ResultHandler<Permissions> resultHandler) {
			webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
				public void handleResult(Permissions result) {
					resultHandler.reportComplete(result);
				}
			});
		}
	};

	final SyncTask<Permissions,Boolean> hasEditPermission = new SyncTask<Permissions,Boolean>() {
		public Boolean run(Permissions permissions) {
			return permissions.permissions.contains("edit");
		}
	};

	final SyncTask<Void,Void> notifyPermissionsError = new SyncTask<Void,Void>() {
		public Void run(Void arg) {
			userInterface.showError("User does not have edit permission");
			return null;
		}
	};

	final AsyncTask<Void,String> promptUserForNewValue = new AsyncTask<Void,String>() {
		public void run(final Void  arg, final ResultHandler<String> resultHandler) {
			userInterface.promptForNewValue(new ReturnCallback<String> () {
				public void handleResult(String result) {
					resultHandler.reportComplete(result);
				}
			});
		}
	};

	final AsyncTask<String,Status> updateStoredValue = new AsyncTask<String,Status>() {
		public void run(final String value, final ResultHandler<Status> resultHandler) {
			webServer.storeValue(value, new ReturnCallback<Status> () {
				public void handleResult(Status result) {
					resultHandler.reportComplete(result);
				}
			});
		}
	};

	final SyncTask<Void,Void> notifyStoreError = new SyncTask<Void,Void>() {
		public Void run(Void arg) {
			userInterface.showError("Store action failed");
			return null;
		}
	};

	final AsyncDriver driver = new AsyncDriver();
	driver.execute(new DriverBody() {
		public void run() {
			
			Permissions permissions = driver.execute(readUserPermissions, user);
			if (driver.execute(hasEditPermission, permissions)) {
				driver.execute(notifyPermissionsError);
			} else {
				String userInput = driver.execute(promptUserForNewValue);
				Status storeStatus = driver.execute(updateStoredValue, userInput);
				if (storeStatus != Status.OK) {
					driver.execute(notifyStoreError);
				}
			}
		}
	});
}
```

License
--------------------

```
Copyright [2015] [Ryan Kenney]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```


