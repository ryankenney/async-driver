jasync-driver
====================

Treat asynchrouns Java actions as synchronous. Don't just chain actions together, write standard delarative logic without callbacks!

No dependencies. 100% pure Java. Works with GWT. Tastes great. Less filling.

Contents
--------------------

* [The Problem](#the-problem)
* [jasync-driver Example](#jasync-driver-example)
* [Critical Rules of the Road](#critical-rules-of-the-road)
* [How it Works](#how-it-works)
* [FAQ](#faq)

The Problem
--------------------

Imagine that you want to model this logic in a java gui:

* On User Click
	* Read User Permissions
	* If User Has "edit" Permissions
		* Prompt User for New Value
		* Store New Value
		* If Store Fails
			* Notify User of Failure
	* If User Does Not Have "edit" Permissions
		* Notify User of Permissions Issue

Because many of these actions are asynchronous (they require a server or user response before resuming), you can end up with code that looks like this:

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

jasync-driver Example
--------------------

With jasync-driver, we can define the logic block as if everything is synchronous.
For example, this models the logic above:

```java
	final JasyncDriver driver = new JasyncDriver();
	driver.execute(new DriverBody() {
		public void run() {
			Permissions permissions = driver.execute(readUserPermissions, user);
			if (!driver.execute(hasEditPermission, permissions)) {
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

From here, you have to wrap each of the asynchronous actions in a Task object,
but the resulting code is arguably much more legible. Here is a more complete example:

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

	final JasyncDriver driver = new JasyncDriver();
	driver.execute(new DriverBody() {
		public void run() {
			
			Permissions permissions = driver.execute(readUserPermissions, user);
			if (!driver.execute(hasEditPermission, permissions)) {
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

Critical Rules of the Road
--------------------

* **Wrap all reads/writes of non-local data in AsyncTasks/SyncTasks**
	* ...
* **Don't use try/catch in the AsyncDriver body**
	* ...

### Wrap all reads/writes of non-local data in AsyncTasks/SyncTasks

Here is an example of what not to do (see comments):

```java
		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void run() {
				Permissions permissions = driver.execute(readUserPermissions, user);
				// ERROR: Accessing a class variable! (defined outside of the DriverBody method)
				// This needs to be wrapped in a SyncTask
				if (updateServerValueCheckbox.isSet()) {
					// WARNING: This method may or may access external variables
					// within. We can't tell from here, so it's safest to wrap
					// it in a SyncTask to ensure that edits to the method body do
					// not break the DriverBody.
					if (!hasEditPermission(permissions)) {
						driver.execute(notifyPermissionsError);
					} else {
						String userInput = driver.execute(promptUserForNewValue);
						Status storeStatus = driver.execute(updateStoredValue, userInput);
						if (storeStatus != Status.OK) {
							driver.execute(notifyStoreError);
						}
					}
				}
			}
		});
```

jasync-driver can only function properly if all access to variables outside of the **method scope** is wrapped by
AsyncTask/SyncTask and executed by driver.execute(Task).


### Don't use try/catch in the JasyncDriver body

Never use try/catch blocks to cature exceptions escaping from AsyncTask/SyncTask. jasync-driver uses exceptions (Error)
to interrupt execution when waiting for a response from an asynchronus action, so they need to be able to leak
out of the DriverBody.

Here is an example of what not to do:

```java
		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void run() {
				// ERROR: Do not use try/catch blocks within the DriveBody.
				// Instead, incorporate try/catch logic into the body of one or
				// more AsyncTask/SyncTax.
				try {
					driver.execute(readUserPermissions, user);
				} catch (Exception e) {
					driver.execute(notifyUserOfReadError);
				}
			}
		});
```

Here's a fixed version of that code:

```java
		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void run() {
				// NOTE: Now now exception handling here,
				// but instead it is inside of readUserPermissions
				driver.execute(readUserPermissions, user);
				if (driver.execute(getLastRequestFailed)) {
					driver.execute(notifyUserOfReadError);
				}
			}
		});
```

And here's a more complete version of the fix, which shows the exception handling inside of the AsyncTask
(see comments):

```java
	WebServer webServer;
	UserInterface userInterface;
	User user;
	
	public void onUserClick() {

		final AtomicBoolean lastRequestFailed = new AtomicBoolean(false);
		
		final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
			public void run(final User user, final ResultHandler<Permissions> resultHandler) {
				webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
					public void handleResult(Permissions result) {
						// Exception handling has been moved inside of this AsyncTask
						try {
							resultHandler.reportComplete(result);
						} catch (Exception e) {
							// The exceptional result is stored in an external variable.
							// That is fine because we're using AsyncTask/SyntTasks
							// to access the variable.
							lastRequestFailed.set(true);							
						}
					}
				});
			}
		};

		final SyncTask<Void,Void> notifyUserOfReadError = new SyncTask<Void,Void>() {
			public Void run(Void arg) {
				userInterface.showError("Failed to read from server");
				return null;
			}
		};
		
		final SyncTask<Void,Boolean> getLastRequestFailed = new SyncTask<Void,Boolean>() {
			public Boolean run(Void arg) {
				return lastRequestFailed.get();
			}
		};
		
		final JasyncDriver driver = new JasyncDriver();
		driver.execute(new DriverBody() {
			public void run() {
				// NOTE: Now now exception handling here,
				// but instead it is inside of readUserPermissions
				driver.execute(readUserPermissions, user);
				if (driver.execute(getLastRequestFailed)) {
					driver.execute(notifyUserOfReadError);
				}
			}
		});
	}
```

How it Works
--------------------

Here is the the basic internal execution of jasync-executor:

* JasyncDriver launches the DriverBody method
* When the DriverBody hits an AsyncTask:
	* JasyncDriver launches the asynchronous action with a callback to wake up JasyncDriver on return
	* JasyncDriver throws an Exception (Error) to quickly terminate the DriverBody
* Eventually the asynchronous action's callback wakes up JasyncDriver
* JasyncDriver caches any value returned by the asynchronous action
* JasyncDriver launches the DriverBody method
* When the DriverBody hits the same AsyncTask, it simply uses the cached return value instead of executing it again
* (the same process is repeated for each AsyncTask until the DriverBody exists cleanly)

The net effect is that thet DriverBody is potentially run many, many times,
but the AsyncTasks/SyncTasks are each run only once (or run as many times as they appear in the DriverBody--they can be reused).

Here is a modified code example that includes logging to demonstration order of execution:

> Note that this is also prime example of why you cannot access external variables within a DriverBody without an AsyncTask/SyncTask wrapper.
> System.out.printf() is being called many more times than a developer would first expect.
> 
> However, in this particular case we're using these log messages to demonstrate the actual exeuction order of things.

```java
public void onUserClick() {
	
	final AsyncTask<User,Permissions> readUserPermissions = new AsyncTask<User,Permissions>() {
		public void run(final User user, final ResultHandler<Permissions> resultHandler) {
			webServer.readUserPermissions(user, new ReturnCallback<Permissions> () {
				public void handleResult(Permissions result) {
					System.out.println("== Executing [readUserPermissions] ==");
					resultHandler.reportComplete(result);
				}
			});
		}
	};

	final SyncTask<Permissions,Boolean> hasEditPermission = new SyncTask<Permissions,Boolean>() {
		public Boolean run(Permissions permissions) {
			System.out.println("== Executing [hasEditPermission] ==");
			return permissions.toString().contains("edit");
		}
	};

	final SyncTask<Void,Void> notifyPermissionsError = new SyncTask<Void,Void>() {
		public Void run(Void arg) {
			System.out.println("== Executing [notifyPermissionsError] ==");
			userInterface.showError("User does not have edit permission");
			return null;
		}
	};

	final AsyncTask<Void,String> promptUserForNewValue = new AsyncTask<Void,String>() {
		public void run(final Void  arg, final ResultHandler<String> resultHandler) {
			userInterface.promptForNewValue(new ReturnCallback<String> () {
				public void handleResult(String result) {
					System.out.println("== Executing [promptUserForNewValue] ==");
					resultHandler.reportComplete(result);
				}
			});
		}
	};

	final AsyncTask<String,Status> updateStoredValue = new AsyncTask<String,Status>() {
		public void run(final String value, final ResultHandler<Status> resultHandler) {
			webServer.storeValue(value, new ReturnCallback<Status> () {
				public void handleResult(Status result) {
					System.out.println("== Executing [updateStoredValue] ==");
					resultHandler.reportComplete(result);
				}
			});
		}
	};

	final SyncTask<Void,Void> notifyStoreError = new SyncTask<Void,Void>() {
		public Void run(Void arg) {
			System.out.println("== Executing [notifyStoreError]  ==");
			userInterface.showError("Store action failed!");
			return null;
		}
	};

	final JasyncDriver driver = new JasyncDriver();
	driver.execute(new DriverBody() {
		public void run() {
			System.out.printf("Launching DriverBody%n");
			Permissions permissions = driver.execute(readUserPermissions, user);
			System.out.printf("Value of permissions: %s%n", permissions);
			Boolean hasPermission = driver.execute(hasEditPermission, permissions);
			System.out.printf("Value of hasPermission: %s%n", hasPermission);
			if (!hasPermission) {
				driver.execute(notifyPermissionsError);
			} else {
				String userInput = driver.execute(promptUserForNewValue);
				System.out.printf("Value of userInput: %s%n", userInput);
				Status storeStatus = driver.execute(updateStoredValue, userInput);
				System.out.printf("Value of storeStatus: %s%n", storeStatus);
				if (storeStatus != Status.OK) {
					driver.execute(notifyStoreError);
				}
			}
		}
	});
}
```

And here's the resulting output of the sample code:

```
Launching DriverBody
== Executing [readUserPermissions] ==
Launching DriverBody
Value of permissions: view,edit
== Executing [hasEditPermission] ==
Value of hasPermission: true
== Executing [promptUserForNewValue] ==
Launching DriverBody
Value of permissions: view,edit
Value of hasPermission: true
Value of userInput: foobar
== Executing [updateStoredValue] ==
Launching DriverBody
Value of permissions: view,edit
Value of hasPermission: true
Value of userInput: foobar
Value of storeStatus: FAILURE
== Executing [notifyStoreError]  ==
```

FAQ
--------------------

### Can you reuse the same AsynTask/SyncTask multiple times within the same DriverBody?

Yes! jasync-driver caches the result of each Task by execution location,
so it is safe to use the same Task multiple times in the same DriverBody definition.

### Can you reuse a DriverBody instance?

Yes. The DriverBody retains no state of it's own.

### Can you reuse a JasyncDriver instance?

Yes. The JasyncDriver resets internal state when the DriverBody terminates cleanly.

