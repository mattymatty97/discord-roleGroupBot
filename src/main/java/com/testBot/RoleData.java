package com.testBot;

public class RoleData {
    private String roleName;
    private Long roleId;

    public String getRoleName() {
        return roleName;
    }

    public Long getRoleId() {
        return roleId;
    }

    public RoleData(String roleName, Long roleId) {
        this.roleName = roleName;
        this.roleId = roleId;
    }
}
