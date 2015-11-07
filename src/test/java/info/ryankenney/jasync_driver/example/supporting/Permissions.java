package info.ryankenney.jasync_driver.example.supporting;


public class Permissions {
	
	private String permissionsString;
	
	Permissions(String permissionsString) {
		this.permissionsString = permissionsString;
	}
	
	public String toString() {
		return permissionsString;
	}
}
