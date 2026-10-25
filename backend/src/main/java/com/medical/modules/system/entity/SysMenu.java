package com.medical.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {

    /** 类型：目录 */
    public static final int TYPE_DIR = 1;
    /** 类型：菜单 */
    public static final int TYPE_MENU = 2;
    /** 类型：按钮 */
    public static final int TYPE_BUTTON = 3;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String menuName;

    private Long parentId;

    private String path;

    private String perms;

    private String icon;

    private Integer menuType;

    private Integer sort;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
