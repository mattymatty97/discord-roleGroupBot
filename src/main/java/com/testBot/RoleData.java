package com.testBot;

import java.util.List;

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

    public static RoleData find(List<RoleData> list,Long id)
    {
        for (RoleData role : list) {
            if (role.getRoleId().equals(id))
                return role;
        }

        return null;
    }
    public static RoleData find(List<RoleData> list,String name)
    {
        for (RoleData role : list)
        {
            if(role.getRoleName().equals(name))
                return role;
        }
        return null;
    }
}
