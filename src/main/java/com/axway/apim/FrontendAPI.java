package com.axway.apim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FrontendAPI {

	private String id;
	private String apiId;
	private String organizationId;
	private String name;
	private String state;
	private String version;
	private String path;
	private String createdBy;

	private List<CACert> caCerts;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getApiId() {
		return apiId;
	}

	public void setApiId(String apiId) {
		this.apiId = apiId;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public List<CACert> getCaCerts() {
		return caCerts;
	}

	public void setCaCerts(List<CACert> caCerts) {
		this.caCerts = caCerts;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj instanceof FrontendAPI) {
			FrontendAPI frontendAPI = (FrontendAPI) obj;
			if (id.equals(frontendAPI.getId()))
				return true;

		}
		return false;

	}

}