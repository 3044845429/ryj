# 数据恢复说明

在已执行 `schema.sql` 建好表的前提下，用以下方式之一恢复数据。

## 方式一：一键恢复（推荐）

执行合并脚本，一次写入所有种子与演示数据（**所有账号密码均为 123456**）：

```bash
# 在项目根目录 e:\bs 下执行（按提示输入 root 密码）
mysql -u root -p bb < database/restore_all.sql
```

Windows PowerShell 若 `<` 重定向有问题，可用：

```powershell
Get-Content database\restore_all.sql -Raw | mysql -u root -p bb
```

## 方式二：分步执行原有脚本

若希望分步执行，请**严格按顺序**执行（且注意：`insert_job_postings.sql` 与 `restore_all.sql` 不要混用，二选一）：

1. **必做**：`seed_after_schema.sql` — 基础账号与就业政策  
2. **可选**：`insert_public_homepage_data.sql` — 首页通知、公共资料、搜索历史（会覆盖 teacher id=1，与 seed 冲突）  
3. **可选**：`insert_job_postings.sql` — 企业与岗位（会覆盖 employer id=1，与 seed 冲突）  
4. **可选**：`insert_dynamics_data.sql` — 资讯动态页示例数据（需存在 `sys_user.id=1`，如管理员账号）

因此更推荐使用 **方式一** 的 `restore_all.sql`，已处理好 ID 冲突。

## 恢复后验证

- 登录：`admin` / `student1` / `teacher1` / `employer1`（密码均为 `123456`）  
- 首页应有通知、公共资料；岗位列表应有约 20 条职位；就业政策页应有 4 条政策（若已启用该功能）。

## 脚本内容概要

`restore_all.sql` 包含：

- 用户：admin、student1、teacher1、employer1、admin_portal、4 名教师、3 名学生、8 家企业用户  
- 学生档案、教师、企业信息  
- 20 条招聘职位及职位要求  
- 系统通知、公共资料、搜索历史  
- 就业政策（含建表，若表中不存在）  
- 自增 ID 起点设置  

若表结构来自 `schema.sql` 且未改表名/字段，直接执行即可。
