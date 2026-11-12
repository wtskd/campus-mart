-- =====================================================================
-- 补充前端下拉框使用的系统字典（sys_normal_disable / sys_user_gender）
-- 背景：管理后台的「用户管理 / 角色管理 / 轮播管理 / 公告管理」状态筛选
--       用的是 <DictSelect dict-type="sys_normal_disable">，而 01-schema.sql
--       的种子数据里漏了这两个字典类型，导致下拉框显示「无数据」。
-- 说明：可重复执行（先按 dict_type 删除再插入）。
-- =====================================================================

USE campus_mart;

-- 1. 启用/停用字典（对应后端 status: '0' 正常、'1' 停用）
DELETE FROM sys_dict_data WHERE dict_type = 'sys_normal_disable';
DELETE FROM sys_dict_type WHERE dict_type = 'sys_normal_disable';
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
  ('系统开关', 'sys_normal_disable', '0', '用户/角色/轮播/公告等通用启用停用状态');
SET @dict_normal = LAST_INSERT_ID();
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, is_default, status) VALUES
  ('sys_normal_disable', '正常', '0', 1, 'success', 'Y', '0'),
  ('sys_normal_disable', '停用', '1', 2, 'danger',  'N', '0');

-- 2. 性别字典（sys_user.gender: 0 未知、1 男、2 女）
DELETE FROM sys_dict_data WHERE dict_type = 'sys_user_gender';
DELETE FROM sys_dict_type WHERE dict_type = 'sys_user_gender';
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
  ('用户性别', 'sys_user_gender', '0', '用户管理列表展示');
SET @dict_gender = LAST_INSERT_ID();
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, is_default, status) VALUES
  ('sys_user_gender', '未知', '0', 1, 'info',    'Y', '0'),
  ('sys_user_gender', '男',   '1', 2, 'primary', 'N', '0'),
  ('sys_user_gender', '女',   '2', 3, 'danger',  'N', '0');

-- 3. 结果自检
SELECT t.dict_type, t.dict_name, COUNT(d.dict_code) AS data_count
FROM sys_dict_type t
LEFT JOIN sys_dict_data d ON d.dict_type = t.dict_type
WHERE t.dict_type IN ('sys_normal_disable', 'sys_user_gender')
GROUP BY t.dict_type, t.dict_name;

-- 4. 评价状态字典（biz_comment.status: 0 正常、1 已隐藏、2 审核未通过）
--    后台「评价管理」的状态筛选原来错用了 sys_normal_disable（正常/停用），语义对不上。
DELETE FROM sys_dict_data WHERE dict_type = 'item_comment_status';
DELETE FROM sys_dict_type WHERE dict_type = 'item_comment_status';
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
  ('评价状态', 'item_comment_status', '0', '后台评价管理的状态筛选与展示');
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, list_class, is_default, status) VALUES
  ('item_comment_status', '正常',       '0', 1, 'success', 'Y', '0'),
  ('item_comment_status', '已隐藏',     '1', 2, 'info',    'N', '0'),
  ('item_comment_status', '审核未通过', '2', 3, 'danger',  'N', '0');

-- 5. 结果自检
SELECT t.dict_type, t.dict_name, COUNT(d.dict_code) AS data_count
FROM sys_dict_type t
LEFT JOIN sys_dict_data d ON d.dict_type = t.dict_type
WHERE t.dict_type IN ('sys_normal_disable', 'sys_user_gender', 'item_comment_status')
GROUP BY t.dict_type, t.dict_name;
