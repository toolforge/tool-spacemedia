package org.wikimedia.commons.donvip.spacemedia.data.domain.dvids.api;

public class ApiErrors {

    private ApiError branch;

    private ApiError unit;

    public ApiError getBranch() {
        return branch;
    }

    public void setBranch(ApiError branch) {
        this.branch = branch;
    }

    public ApiError getUnit() {
        return unit;
    }

    public void setUnit(ApiError unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "ApiErrors [" + (branch != null ? "branch=" + branch + ", " : "") + (unit != null ? "unit=" + unit : "")
                + "]";
    }
}
