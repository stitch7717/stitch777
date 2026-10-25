package com.medical.modules.system.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点
 */
@Data
public class MenuVO {

    private Long id;
    private String menuName;
    private Long parentId;
    private String path;
    private String perms;
    private String icon;
    private Integer menuType;
    private Integer sort;
    private List<MenuVO> children = new ArrayList<>();
}
