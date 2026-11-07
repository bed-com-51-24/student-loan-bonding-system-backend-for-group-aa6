package mw.loancompliance.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class OrganizationPatchRequest {
    @Size(max = 200) private String legalName;
    @Size(max = 100) private String registrationNumber;
    @Size(max = 100) private String sector;
    private String address;
    @Size(max = 30) private String phone;
    @Email @Size(max = 255) private String email;
    public String getLegalName() { return legalName; }
    public void setLegalName(String v) { legalName = v; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String v) { registrationNumber = v; }
    public String getSector() { return sector; }
    public void setSector(String v) { sector = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { address = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
}
