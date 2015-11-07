package info.ryankenney.jasync_driver.example.supporting;


public interface UserInterface {
	void showError(String message);
	void promptForNewValue(ReturnCallback<String> returnCallback);
}
