package ps.sample.test.utils;

public enum Feature {

	INVESTMENT("investment"),
	EMPLOYEE_MASTER("employee"),
	PAYROLL("payroll");
	
	private String feature;
	
	private Feature(String feature) {
		this.feature = feature;
	}
	
	public String getFeature() {
		return feature;
	}
}
