package com.ruoyi.system.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.domain.SysDept;
import com.ruoyi.system.api.domain.SysRole;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 瑙掕壊淇℃伅
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/role")
public class SysRoleController extends BaseController
{
    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysDeptService deptService;

    @RequiresPermissions("system:role:list")
    @GetMapping("/list")
    public TableDataInfo list(SysRole role)
    {
        startPage();
        List<SysRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }

    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:role:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysRole role)
    {
        List<SysRole> list = roleService.selectRoleList(role);
        ExcelUtil<SysRole> util = new ExcelUtil<SysRole>(SysRole.class);
        util.exportExcel(response, list, "瑙掕壊鏁版嵁");
    }

    /**
     * 鏍规嵁瑙掕壊缂栧彿鑾峰彇璇︾粏淇℃伅
     */
    @RequiresPermissions("system:role:query")
    @GetMapping(value = "/{roleId}")
    public AjaxResult getInfo(@PathVariable("roleId") String roleId)
    {
        Long parsedRoleId = Convert.toLong(roleId);
        roleService.checkRoleDataScope(parsedRoleId);
        return success(roleService.selectRoleById(parsedRoleId));
    }

    /**
     * 鏂板瑙掕壊
     */
    @RequiresPermissions("system:role:add")
    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysRole role)
    {
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("鏂板瑙掕壊'" + role.getRoleName() + "'澶辫触锛岃鑹插悕绉板凡瀛樺湪");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("鏂板瑙掕壊'" + role.getRoleName() + "'澶辫触锛岃鑹叉潈闄愬凡瀛樺湪");
        }
        role.setCreateBy(SecurityUtils.getUsername());
        return toAjax(roleService.insertRole(role));

    }

    /**
     * 淇敼淇濆瓨瑙掕壊
     */
    @RequiresPermissions("system:role:edit")
    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("淇敼瑙掕壊'" + role.getRoleName() + "'澶辫触锛岃鑹插悕绉板凡瀛樺湪");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("淇敼瑙掕壊'" + role.getRoleName() + "'澶辫触锛岃鑹叉潈闄愬凡瀛樺湪");
        }
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRole(role));
    }

    /**
     * 淇敼淇濆瓨鏁版嵁鏉冮檺
     */
    @RequiresPermissions("system:role:edit")
    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.UPDATE)
    @PutMapping("/dataScope")
    public AjaxResult dataScope(@RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        return toAjax(roleService.authDataScope(role));
    }

    /**
     * 鐘舵€佷慨鏀?     */
    @RequiresPermissions("system:role:edit")
    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        role.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    /**
     * 鍒犻櫎瑙掕壊
     */
    @RequiresPermissions("system:role:remove")
    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable("roleIds") String roleIds)
    {
        return toAjax(roleService.deleteRoleByIds(Convert.toLongArray(roleIds)));
    }

    /**
     * 鑾峰彇瑙掕壊閫夋嫨妗嗗垪琛?     */
    @RequiresPermissions("system:role:query")
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        return success(roleService.selectRoleAll());
    }
    /**
     * 鏌ヨ宸插垎閰嶇敤鎴疯鑹插垪琛?     */
    @RequiresPermissions("system:role:list")
    @GetMapping("/authUser/allocatedList")
    public TableDataInfo allocatedList(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectAllocatedList(user);
        return getDataTable(list);
    }

    /**
     * 鏌ヨ鏈垎閰嶇敤鎴疯鑹插垪琛?     */
    @RequiresPermissions("system:role:list")
    @GetMapping("/authUser/unallocatedList")
    public TableDataInfo unallocatedList(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUnallocatedList(user);
        return getDataTable(list);
    }

    /**
     * 鍙栨秷鎺堟潈鐢ㄦ埛
     */
    @RequiresPermissions("system:role:edit")
    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/cancel")
    public AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole)
    {
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    /**
     * 鎵归噺鍙栨秷鎺堟潈鐢ㄦ埛
     */
    @RequiresPermissions("system:role:edit")
    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/cancelAll")
    public AjaxResult cancelAuthUserAll(@RequestParam("roleId") String roleId, @RequestParam("userIds") String userIds)
    {
        return toAjax(roleService.deleteAuthUsers(Convert.toLong(roleId), Convert.toLongArray(userIds)));
    }

    /**
     * 鎵归噺閫夋嫨鐢ㄦ埛鎺堟潈
     */
    @RequiresPermissions("system:role:edit")
    @Log(title = "瑙掕壊绠＄悊", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/selectAll")
    public AjaxResult selectAuthUserAll(@RequestParam("roleId") String roleId, @RequestParam("userIds") String userIds)
    {
        Long parsedRoleId = Convert.toLong(roleId);
        roleService.checkRoleDataScope(parsedRoleId);
        return toAjax(roleService.insertAuthUsers(parsedRoleId, Convert.toLongArray(userIds)));
    }

    /**
     * 鑾峰彇瀵瑰簲瑙掕壊閮ㄩ棬鏍戝垪琛?     */
    @RequiresPermissions("system:role:query")
    @GetMapping(value = "/deptTree/{roleId}")
    public AjaxResult deptTree(@PathVariable("roleId") String roleId)
    {
        Long parsedRoleId = Convert.toLong(roleId);
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(parsedRoleId));
        ajax.put("depts", deptService.selectDeptTreeList(new SysDept()));
        return ajax;
    }
}
