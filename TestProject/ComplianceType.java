package com.ps.util;

/**
 * ComplianceType to hold the compliance types
 * @author anand nandeshwar
 * @since 20-09-2020
 *
 */
public enum ComplianceType {

	PF("Provident Fund"),
	EPS("Employee Pension Scheme"),
	GRATUITY("Gratuity"),
	SA("Super Annuation"),
	ESIC("Employee State Insurance"),
	TDS("Tax Deduction at Source"),
	PT("Professional Tax"),
	LWF("Labour Welfare Fund");
	
	private String complianceType;
	
	private ComplianceType(String complianceType) {
		this.complianceType = complianceType;
	}
	
	public String getComplianceType() {
		return complianceType;
	}
}
