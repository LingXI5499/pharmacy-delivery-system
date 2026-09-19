-- 速安药房 V2 虚构演示种子。
-- 仅供教学 / 作品集：禁止对接真实患者、处方原件、医保或支付。
-- 在 Flyway V1～V5 应用到空库 pharmacy_delivery_demo 之后执行。
-- 不写入 Flyway 迁移，避免污染 CI 空库路径。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 账号：密码均为 123456。
-- 哈希由 Spring BCryptPasswordEncoder(12) 生成（$2a$）。
-- 不使用 E1 的 $2b$ 串：本仓库 PasswordEncoder 对该串 matches 为 false。
-- ---------------------------------------------------------------------------
INSERT INTO sys_user (username, password, nickname, phone, role, status) VALUES
('admin',      '$2a$12$L0uddimNhfuvjTmraL7NIup0KsDvxA.F5Rcvz3v9B7DXuYr9zjfx.', '店长·陈安',     '13800001001', 'ADMIN', 1),
('user01',     '$2a$12$L0uddimNhfuvjTmraL7NIup0KsDvxA.F5Rcvz3v9B7DXuYr9zjfx.', '顾客·林晓舟',   '13800001002', 'USER', 1),
('user02',     '$2a$12$L0uddimNhfuvjTmraL7NIup0KsDvxA.F5Rcvz3v9B7DXuYr9zjfx.', '顾客·周敏',     '13800001003', 'USER', 1),
('user03',     '$2a$12$L0uddimNhfuvjTmraL7NIup0KsDvxA.F5Rcvz3v9B7DXuYr9zjfx.', '顾客·黄志远',   '13800001004', 'USER', 1),
('pharmacist', '$2a$12$L0uddimNhfuvjTmraL7NIup0KsDvxA.F5Rcvz3v9B7DXuYr9zjfx.', '药师·王晓兰',   '13800001005', 'PHARMACIST', 1),
('purchaser',  '$2a$12$L0uddimNhfuvjTmraL7NIup0KsDvxA.F5Rcvz3v9B7DXuYr9zjfx.', '采购·刘振华',   '13800001006', 'PURCHASER', 1),
('warehouse',  '$2a$12$L0uddimNhfuvjTmraL7NIup0KsDvxA.F5Rcvz3v9B7DXuYr9zjfx.', '仓管·赵建国',   '13800001007', 'WAREHOUSE', 1),
('rider01',    '$2a$12$L0uddimNhfuvjTmraL7NIup0KsDvxA.F5Rcvz3v9B7DXuYr9zjfx.', '骑手·李骑手',   '13800001008', 'RIDER', 1);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = u.role
WHERE u.username IN ('admin','user01','user02','user03','pharmacist','purchaser','warehouse','rider01');

INSERT INTO user_address (user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default) VALUES
((SELECT id FROM sys_user WHERE username = 'user01'), '林晓舟', '13800001002', '广东省', '惠州市', '惠城区', '演达大道速安小区 12 栋 3 单元 502', 1),
((SELECT id FROM sys_user WHERE username = 'user01'), '林晓舟（公司）', '13800001002', '广东省', '惠州市', '惠城区', '江北文昌一路虚构科技园 A 座 8 楼', 0),
((SELECT id FROM sys_user WHERE username = 'user02'), '周敏', '13800001003', '广东省', '惠州市', '惠阳区', '淡水虚构花园 6 号楼 1 单元 201', 1),
((SELECT id FROM sys_user WHERE username = 'user03'), '黄志远', '13800001004', '广东省', '惠州市', '惠城区', '南坛步行街速安巷 18 号', 1),
((SELECT id FROM sys_user WHERE username = 'admin'), '陈安', '13800001001', '广东省', '惠州市', '惠城区', '河南岸药店值班宿舍 1 号', 1);

INSERT INTO delivery_rider (rider_name, phone, status, remark) VALUES
('李骑手', '13900002001', 1, '教学演示：可用'),
('王骑手', '13900002002', 1, '教学演示：可用'),
('赵骑手', '13900002003', 0, '教学演示：停用，不应出现在派单列表');

-- ---------------------------------------------------------------------------
-- 分类与商品（通用名 + 虚构规格，非真实药店进销存导出）
-- ---------------------------------------------------------------------------
INSERT INTO medicine_category (category_name, description, sort_no, status, is_deleted) VALUES
('感冒咳嗽', '虚构教学：感冒、咽痛、止咳', 10, 1, 0),
('解热镇痛', '虚构教学：发热与轻中度疼痛', 20, 1, 0),
('消化系统', '虚构教学：胃部不适、腹泻、便秘', 30, 1, 0),
('抗过敏', '虚构教学：过敏性鼻炎与皮肤过敏', 40, 1, 0),
('维生素矿物质', '虚构教学：营养补充', 50, 1, 0),
('外用皮肤', '虚构教学：皮肤与创口护理', 60, 1, 0),
('慢病常用', '虚构教学：处方慢病用药，须药师审核', 70, 1, 0),
('抗感染处方', '虚构教学：抗菌药物，须药师审核', 80, 1, 0);

INSERT INTO medicine (
  category_id, medicine_name, description, usage_instruction, precautions, price, stock, warning_stock,
  prescription_required, status, version, is_deleted
) VALUES
((SELECT id FROM medicine_category WHERE category_name = '感冒咳嗽'),
 '速安感冒灵颗粒 10g×9 袋', '虚构 OTC 感冒症状缓解颗粒', '开水冲服，一日 3 次', '教学数据，非诊疗建议', 18.80, 0, 8, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '感冒咳嗽'),
 '速安氨酚黄那敏片 10 片', '虚构 OTC 复方感冒片', '一次 1 片，一日 3 次', '教学数据，含日晖成分提示仅作演示', 12.50, 0, 6, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '感冒咳嗽'),
 '速安右美沙芬口服液 120ml', '虚构 OTC 干咳镇咳', '一次 10ml，一日 3 次', '教学数据', 22.00, 0, 5, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '感冒咳嗽'),
 '速安板蓝根颗粒 10g×20 袋', '虚构 OTC 清热演示品（含过期批次）', '开水冲服', '教学数据；过期批次不可售', 16.90, 0, 10, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '解热镇痛'),
 '速安对乙酰氨基酚片 0.5g×12 片', '虚构 OTC 解热镇痛', '一次 1 片，间隔 6 小时以上', '教学数据', 9.90, 0, 12, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '解热镇痛'),
 '速安布洛芬缓释胶囊 0.3g×20 粒', '虚构 OTC，含近效期批次用于 FEFO/近效期页', '一次 1 粒，一日 2 次', '教学数据', 19.80, 0, 8, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '解热镇痛'),
 '速安布洛芬混悬滴剂 20ml', '虚构 OTC 儿童剂型演示', '按体重折算，演示勿当真', '教学数据', 28.50, 0, 4, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '消化系统'),
 '速安蒙脱石散 3g×10 袋', '虚构 OTC 急性腹泻吸附剂', '一次 1 袋，一日 3 次', '教学数据', 15.80, 0, 8, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '消化系统'),
 '速安奥美拉唑肠溶胶囊 20mg×14 粒', '虚构 OTC 烧心演示', '一次 1 粒，一日 1 次', '教学数据', 24.60, 0, 6, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '消化系统'),
 '速安开塞露 20ml×2 支', '虚构 OTC 便秘演示', '直肠给药', '教学数据', 8.50, 0, 10, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '消化系统'),
 '速安多潘立酮片 10mg×30 片', '虚构 OTC 腹胀演示', '餐前 15～30 分钟', '教学数据', 13.20, 0, 6, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '抗过敏'),
 '速安氯雷他定片 10mg×6 片', '虚构 OTC 过敏性鼻炎', '一日 1 片', '教学数据', 16.80, 0, 6, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '抗过敏'),
 '速安西替利嗪片 10mg×12 片', '虚构 OTC 皮肤过敏', '一日 1 片', '教学数据', 18.40, 0, 5, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '维生素矿物质'),
 '速安维生素C片 100mg×100 片', '虚构 OTC，双批次 FEFO', '一日 1～2 片', '教学数据', 14.90, 0, 15, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '维生素矿物质'),
 '速安碳酸钙D3 片 60 片', '虚构 OTC 补钙', '一日 1～2 片', '教学数据', 32.00, 0, 8, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '维生素矿物质'),
 '速安葡萄糖酸锌口服液 10ml×12 支', '虚构 OTC 补锌', '一日 1 支', '教学数据', 26.80, 0, 5, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '维生素矿物质'),
 '速安复合维生素B片 100 片', '虚构 OTC', '一日 1～3 片', '教学数据', 11.50, 0, 8, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '外用皮肤'),
 '速安碘伏消毒液 100ml', '虚构外用消毒', '外用，不可内服', '教学数据', 9.90, 0, 10, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '外用皮肤'),
 '速安创可贴 100 片装', '虚构创口保护', '清洁后贴敷', '教学数据', 12.80, 0, 12, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '外用皮肤'),
 '速安红霉素软膏 1% 10g', '虚构外用', '薄涂患处', '教学数据', 7.50, 0, 8, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '外用皮肤'),
 '速安炉甘石洗剂 100ml', '虚构止痒', '外用，用前摇匀', '教学数据', 10.60, 0, 6, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '外用皮肤'),
 '医用外科口罩 50 只/盒', '虚构防护耗材', '一次性使用', '教学数据', 19.90, 0, 20, 0, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '慢病常用'),
 '速安苯磺酸氨氯地平片 5mg×28 片', '虚构处方：高血压演示', '一日 1 片，遵医嘱', '须药师审核；教学数据', 28.00, 0, 6, 1, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '慢病常用'),
 '速安阿托伐他汀钙片 20mg×7 片', '虚构处方：血脂演示', '一日 1 片，遵医嘱', '须药师审核；教学数据', 35.80, 0, 4, 1, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '慢病常用'),
 '速安盐酸二甲双胍片 0.5g×20 片', '虚构处方：血糖演示', '随餐服用，遵医嘱', '须药师审核；教学数据', 12.40, 0, 6, 1, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '抗感染处方'),
 '速安阿莫西林胶囊 0.25g×24 粒', '虚构处方抗菌', '遵医嘱，完成疗程', '须药师审核；教学数据', 16.50, 0, 8, 1, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '抗感染处方'),
 '速安头孢克肟胶囊 0.1g×6 粒', '虚构处方抗菌', '遵医嘱', '须药师审核；教学数据', 29.90, 0, 4, 1, 1, 0, 0),
((SELECT id FROM medicine_category WHERE category_name = '抗感染处方'),
 '速安连花清瘟胶囊 24 粒', '虚构待检批次演示，默认不可售', '遵说明书', '隔离/待检批次不进入可售聚合', 21.00, 0, 6, 0, 1, 0, 0);

-- ---------------------------------------------------------------------------
-- 批次：可售 = 未过期 + QUALIFIED + sellable=1 + available_qty>0
-- ---------------------------------------------------------------------------
INSERT INTO medicine_batch (
  medicine_id, location_id, batch_no, production_date, expiry_date, purchase_price,
  available_qty, reserved_qty, quality_status, sellable, version
) VALUES
-- 感冒
((SELECT id FROM medicine WHERE medicine_name = '速安感冒灵颗粒 10g×9 袋'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-GANMAO-A', DATE_SUB(CURRENT_DATE, INTERVAL 120 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 300 DAY), 7.20, 48, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安氨酚黄那敏片 10 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-COLD-TAB-A', DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 400 DAY), 4.10, 60, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安右美沙芬口服液 120ml'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-COUGH-A', DATE_SUB(CURRENT_DATE, INTERVAL 60 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 360 DAY), 9.50, 24, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安板蓝根颗粒 10g×20 袋'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-BANLAN-OK', DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 420 DAY), 5.80, 36, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安板蓝根颗粒 10g×20 袋'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-BANLAN-EXP', DATE_SUB(CURRENT_DATE, INTERVAL 400 DAY), DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 5.80, 12, 0, 'QUALIFIED', 0, 0),
-- 解热镇痛 + FEFO 近效期
((SELECT id FROM medicine WHERE medicine_name = '速安对乙酰氨基酚片 0.5g×12 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-PARA-A', DATE_SUB(CURRENT_DATE, INTERVAL 80 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 500 DAY), 3.20, 80, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安布洛芬缓释胶囊 0.3g×20 粒'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-IBU-EARLY', DATE_SUB(CURRENT_DATE, INTERVAL 330 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 18 DAY), 8.40, 10, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安布洛芬缓释胶囊 0.3g×20 粒'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-IBU-LATE', DATE_SUB(CURRENT_DATE, INTERVAL 40 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 320 DAY), 8.60, 40, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安布洛芬混悬滴剂 20ml'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-IBU-DROP', DATE_SUB(CURRENT_DATE, INTERVAL 50 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 280 DAY), 12.00, 18, 0, 'QUALIFIED', 1, 0),
-- 消化
((SELECT id FROM medicine WHERE medicine_name = '速安蒙脱石散 3g×10 袋'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-SMECT-A', DATE_SUB(CURRENT_DATE, INTERVAL 70 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 360 DAY), 6.10, 42, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安奥美拉唑肠溶胶囊 20mg×14 粒'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-OME-A', DATE_SUB(CURRENT_DATE, INTERVAL 100 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY), 9.80, 30, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安开塞露 20ml×2 支'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-ENEMA-A', DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 540 DAY), 2.80, 50, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安多潘立酮片 10mg×30 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-DOMP-A', DATE_SUB(CURRENT_DATE, INTERVAL 110 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 300 DAY), 4.60, 28, 0, 'QUALIFIED', 1, 0),
-- 抗过敏
((SELECT id FROM medicine WHERE medicine_name = '速安氯雷他定片 10mg×6 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-LORAT-A', DATE_SUB(CURRENT_DATE, INTERVAL 55 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 380 DAY), 6.90, 36, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安西替利嗪片 10mg×12 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-CETI-A', DATE_SUB(CURRENT_DATE, INTERVAL 75 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 350 DAY), 7.40, 24, 0, 'QUALIFIED', 1, 0),
-- 维生素 FEFO
((SELECT id FROM medicine WHERE medicine_name = '速安维生素C片 100mg×100 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-VC-EARLY', DATE_SUB(CURRENT_DATE, INTERVAL 200 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 22 DAY), 4.20, 16, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安维生素C片 100mg×100 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-VC-LATE', DATE_SUB(CURRENT_DATE, INTERVAL 40 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 400 DAY), 4.50, 64, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安碳酸钙D3 片 60 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-CAD3-A', DATE_SUB(CURRENT_DATE, INTERVAL 90 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 480 DAY), 12.80, 32, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安葡萄糖酸锌口服液 10ml×12 支'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-ZN-A', DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 260 DAY), 10.20, 20, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安复合维生素B片 100 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-VB-A', DATE_SUB(CURRENT_DATE, INTERVAL 130 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 340 DAY), 3.90, 40, 0, 'QUALIFIED', 1, 0),
-- 外用
((SELECT id FROM medicine WHERE medicine_name = '速安碘伏消毒液 100ml'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-IODINE-A', DATE_SUB(CURRENT_DATE, INTERVAL 25 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 600 DAY), 3.50, 45, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安创可贴 100 片装'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-BAND-A', DATE_SUB(CURRENT_DATE, INTERVAL 15 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 700 DAY), 4.80, 70, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安红霉素软膏 1% 10g'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-ERY-A', DATE_SUB(CURRENT_DATE, INTERVAL 200 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY), 2.40, 22, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安炉甘石洗剂 100ml'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-CALA-A', DATE_SUB(CURRENT_DATE, INTERVAL 80 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 310 DAY), 3.80, 18, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '医用外科口罩 50 只/盒'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-MASK-A', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 800 DAY), 8.00, 120, 0, 'QUALIFIED', 1, 0),
-- 慢病 RX
((SELECT id FROM medicine WHERE medicine_name = '速安苯磺酸氨氯地平片 5mg×28 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-AML-A', DATE_SUB(CURRENT_DATE, INTERVAL 70 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 330 DAY), 11.00, 24, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安阿托伐他汀钙片 20mg×7 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-ATOR-A', DATE_SUB(CURRENT_DATE, INTERVAL 50 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 280 DAY), 14.20, 16, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安盐酸二甲双胍片 0.5g×20 片'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-MET-A', DATE_SUB(CURRENT_DATE, INTERVAL 40 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 360 DAY), 4.10, 36, 0, 'QUALIFIED', 1, 0),
-- 抗感染 RX
((SELECT id FROM medicine WHERE medicine_name = '速安阿莫西林胶囊 0.25g×24 粒'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-AMOX-A', DATE_SUB(CURRENT_DATE, INTERVAL 35 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY), 6.50, 30, 0, 'QUALIFIED', 1, 0),
((SELECT id FROM medicine WHERE medicine_name = '速安头孢克肟胶囊 0.1g×6 粒'),
 (SELECT id FROM inventory_location WHERE location_code = 'MAIN'),
 'SA-CEFIX-A', DATE_SUB(CURRENT_DATE, INTERVAL 25 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 200 DAY), 12.80, 12, 0, 'QUALIFIED', 1, 0),
-- 待检隔离（不计入 medicine.stock）
((SELECT id FROM medicine WHERE medicine_name = '速安连花清瘟胶囊 24 粒'),
 (SELECT id FROM inventory_location WHERE location_code = 'LEGACY_QUARANTINE'),
 'SA-LH-WAIT', DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 360 DAY), 8.80, 20, 0, 'QUARANTINED', 0, 0);

UPDATE medicine m
SET stock = (
  SELECT COALESCE(SUM(b.available_qty), 0)
  FROM medicine_batch b
  WHERE b.medicine_id = m.id
    AND b.sellable = 1
    AND b.quality_status = 'QUALIFIED'
    AND b.expiry_date > CURRENT_DATE
    AND b.available_qty > 0
);

-- ---------------------------------------------------------------------------
-- 供应商与采购单（一份待收货、一份草稿、一份驳回）
-- ---------------------------------------------------------------------------
INSERT INTO supplier (supplier_code, supplier_name, contact_name, phone, status) VALUES
('SA-SUP-001', '惠州速安医药虚构批发', '钱进', '0752-8000001', 1),
('SA-SUP-002', '岭南教学器械耗材', '孙倩', '0752-8000002', 1);

INSERT INTO purchase_order (purchase_no, supplier_id, status, applicant_id, approver_id, approved_time, remark) VALUES
('SA-PO-INBOUND',
 (SELECT id FROM supplier WHERE supplier_code = 'SA-SUP-001'),
 'APPROVED',
 (SELECT id FROM sys_user WHERE username = 'purchaser'),
 (SELECT id FROM sys_user WHERE username = 'admin'),
 DATE_SUB(NOW(), INTERVAL 1 DAY),
 '教学：已批准待仓库收货，收货后可增加口罩与感冒灵库存'),
('SA-PO-DRAFT',
 (SELECT id FROM supplier WHERE supplier_code = 'SA-SUP-002'),
 'DRAFT',
 (SELECT id FROM sys_user WHERE username = 'purchaser'),
 NULL, NULL, '教学：采购草稿，管理员尚未审批'),
('SA-PO-REJECT',
 (SELECT id FROM supplier WHERE supplier_code = 'SA-SUP-002'),
 'REJECTED',
 (SELECT id FROM sys_user WHERE username = 'purchaser'),
 (SELECT id FROM sys_user WHERE username = 'admin'),
 DATE_SUB(NOW(), INTERVAL 2 DAY),
 '[REJECTED] 教学驳回：供应商资质复印件不齐');

INSERT INTO purchase_order_item (purchase_order_id, medicine_id, ordered_qty, received_qty, purchase_price) VALUES
((SELECT id FROM purchase_order WHERE purchase_no = 'SA-PO-INBOUND'),
 (SELECT id FROM medicine WHERE medicine_name = '医用外科口罩 50 只/盒'), 40, 0, 8.00),
((SELECT id FROM purchase_order WHERE purchase_no = 'SA-PO-INBOUND'),
 (SELECT id FROM medicine WHERE medicine_name = '速安感冒灵颗粒 10g×9 袋'), 20, 0, 7.20),
((SELECT id FROM purchase_order WHERE purchase_no = 'SA-PO-DRAFT'),
 (SELECT id FROM medicine WHERE medicine_name = '速安碳酸钙D3 片 60 片'), 24, 0, 12.80),
((SELECT id FROM purchase_order WHERE purchase_no = 'SA-PO-REJECT'),
 (SELECT id FROM medicine WHERE medicine_name = '速安创可贴 100 片装'), 50, 0, 4.80);

INSERT INTO inventory_ledger (
  event_no, business_type, business_id, medicine_id, batch_id,
  available_delta, reserved_delta, available_after, reserved_after, operator_id, reason
)
SELECT
  CONCAT('DEMO-OPEN-', b.id),
  'STOCK_ADJUST',
  'DEMO-OPENING',
  b.medicine_id,
  b.id,
  b.available_qty,
  0,
  b.available_qty,
  0,
  (SELECT id FROM sys_user WHERE username = 'warehouse'),
  CASE
    WHEN b.quality_status = 'QUARANTINED' THEN '教学演示：待检隔离期初，不计入可售聚合'
    WHEN b.sellable = 0 THEN '教学演示：过期批次期初，sellable=0'
    ELSE '教学演示：期初合格可售批次建账'
  END
FROM medicine_batch b;

INSERT INTO shopping_cart (user_id, medicine_id, quantity, selected)
SELECT u.id, m.id, 2, 1
FROM sys_user u
JOIN medicine m ON m.medicine_name = '速安对乙酰氨基酚片 0.5g×12 片'
WHERE u.username = 'user01';

INSERT INTO shopping_cart (user_id, medicine_id, quantity, selected)
SELECT u.id, m.id, 1, 1
FROM sys_user u
JOIN medicine m ON m.medicine_name = '速安维生素C片 100mg×100 片'
WHERE u.username = 'user01';

INSERT INTO inventory_count (count_no, status, remark, created_by)
SELECT 'SA-IC-DRAFT-001', 'DRAFT', '教学：仓库可开始盘点的草稿单', id
FROM sys_user WHERE username = 'warehouse';

SET FOREIGN_KEY_CHECKS = 1;
