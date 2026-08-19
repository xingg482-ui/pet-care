insert into account (username, display_name, password_hash, role, status, created_at, updated_at)
select 'admin',
       '高级管理员',
       'pet-care-seed:6b62b369e83167972b4f29e76f9b667c02a53eb7c7d981cfb210da1412985a57',
       'SUPER_ADMIN',
       'ACTIVE',
       datetime('now', 'localtime'),
       datetime('now', 'localtime')
where not exists (select 1 from account where username = 'admin');

insert into customer (name, phone, email, address, remark)
select '张三', '13800000001', 'zhangsan@example.com', '示例小区 1 号', '演示客户'
where not exists (select 1 from customer where phone = '13800000001');

insert into customer (name, phone, email, address, remark)
select '李四', '13800000002', 'lisi@example.com', '演示花园 2 号', '演示客户'
where not exists (select 1 from customer where phone = '13800000002');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ALL', '服务项目与标准流程', '宠物洗澡服务包含什么？', '宠物洗澡通常包含基础状态观察、梳毛、温水清洗、专用洗护、吹干、耳眼清洁、脚底毛和指甲基础护理。实际服务内容以门店服务项目配置为准，如宠物有皮肤异常、应激或伤口，需要先告知门店人员。', '洗澡,洗护,基础洗护,服务内容,流程', 10
where not exists (select 1 from ai_faq where question = '宠物洗澡服务包含什么？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ALL', '服务项目与标准流程', '宠物美容服务流程是什么？', '美容服务一般包括预约确认、宠物状态检查、造型沟通、洗护吹干、修剪造型、细节清理、完成复核和交付说明。建议客户提前说明宠物性格、过敏情况和期望造型。', '美容,精致美容,造型,修剪,SOP,流程', 20
where not exists (select 1 from ai_faq where question = '宠物美容服务流程是什么？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ALL', '服务项目与标准流程', '驱虫提醒服务主要做什么？', '驱虫提醒用于记录和提醒宠物体内外驱虫时间，帮助客户按周期管理。AI 可以解释记录含义，但具体驱虫药品、剂量和频率应以兽医或产品说明为准。', '驱虫,驱虫提醒,体内驱虫,体外驱虫', 30
where not exists (select 1 from ai_faq where question = '驱虫提醒服务主要做什么？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ALL', '服务项目与标准流程', '疫苗提醒服务主要做什么？', '疫苗提醒用于记录宠物疫苗名称、接种日期和下次提醒时间，帮助客户避免漏打。若宠物刚接种疫苗，通常建议观察精神、食欲和局部反应，如异常应咨询兽医。', '疫苗,疫苗提醒,接种,健康记录', 40
where not exists (select 1 from ai_faq where question = '疫苗提醒服务主要做什么？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ALL', '服务项目与标准流程', '体重记录有什么用？', '体重记录可以帮助观察宠物成长、肥胖、消瘦或健康变化趋势。短期明显增减重可能与饮食、运动、疾病或应激有关，建议结合精神、食欲、排便情况综合判断。', '体重,体重记录,健康记录,肥胖,消瘦', 50
where not exists (select 1 from ai_faq where question = '体重记录有什么用？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '订单与预约', '订单状态“待确认”是什么意思？', '待确认表示客户已提交预约或订单，门店还没有最终确认服务时间或接单。请等待门店处理，如时间较紧可联系门店确认。', '待确认,订单状态,预约状态,PENDING', 100
where not exists (select 1 from ai_faq where question = '订单状态“待确认”是什么意思？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '订单与预约', '订单状态“已确认”是什么意思？', '已确认表示门店已经接受预约，客户可按预约时间到店。建议提前准备宠物牵引绳、航空箱、疫苗或健康信息等必要资料。', '已确认,订单状态,CONFIRMED,预约', 110
where not exists (select 1 from ai_faq where question = '订单状态“已确认”是什么意思？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '订单与预约', '订单状态“服务中”是什么意思？', '服务中表示宠物正在接受对应服务，门店会按服务流程处理。客户如有特殊要求，应尽早联系门店补充说明。', '服务中,订单状态,IN_SERVICE,进度', 120
where not exists (select 1 from ai_faq where question = '订单状态“服务中”是什么意思？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '订单与预约', '已完成未支付订单怎么处理？', '已完成未支付表示服务已经结束但费用还未结清。若系统支持线上支付，可点击支付入口；若暂未接入线上支付，请按门店指引到店或线下付款。', '未支付,待支付,已完成,付款,支付', 130
where not exists (select 1 from ai_faq where question = '已完成未支付订单怎么处理？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '订单与预约', '可以修改预约时间吗？', '如订单尚未开始，通常可以联系门店协商修改预约时间。若订单已进入服务中或已完成，一般不能再修改预约时间。', '修改预约,预约时间,改时间,订单时间', 140
where not exists (select 1 from ai_faq where question = '可以修改预约时间吗？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ALL', '宠物托管', '宠物托管的标准流程是什么？', '托管通常包括预约登记、宠物信息核对、健康状态确认、安排房间或区域、每日喂食补水、活动陪伴、环境清洁、健康观察和接回交付。特殊喂养、用药、胆小或攻击倾向需要提前说明。', '托管,寄养,标准流程,入住,接回', 200
where not exists (select 1 from ai_faq where question = '宠物托管的标准流程是什么？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '宠物托管', '托管期间每天会做哪些照护？', '常见照护包括喂食补水、活动陪伴、位置清洁和健康观察。系统中的每日照护任务会记录完成情况，客户可在托管相关页面查看。', '托管照护,每日照护,喂食,活动,清洁,健康观察', 210
where not exists (select 1 from ai_faq where question = '托管期间每天会做哪些照护？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '宠物托管', '接回宠物时要注意什么？', '接回时建议核对宠物状态、随身物品、托管天数、费用和照护备注。回家后先给宠物安静适应环境，观察食欲、精神和排便。', '接回,退房,取宠物,托管结束', 220
where not exists (select 1 from ai_faq where question = '接回宠物时要注意什么？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ALL', '宠物托管', '托管宠物出现异常怎么办？', '如门店发现宠物精神差、呕吐、腹泻、受伤等异常，应及时联系客户并建议就医。AI 只能提供一般风险提示，不能替代兽医诊断。', '托管异常,呕吐,腹泻,受伤,精神差,就医', 230
where not exists (select 1 from ai_faq where question = '托管宠物出现异常怎么办？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '健康记录与科学养宠', '疫苗后多久可以洗澡？', '一般建议疫苗后观察 7 天左右，确认精神、食欲正常且没有明显不适后再洗澡。不同宠物情况不同，如有异常反应请先咨询兽医。', '疫苗后洗澡,打疫苗,洗澡,疫苗', 300
where not exists (select 1 from ai_faq where question = '疫苗后多久可以洗澡？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '健康记录与科学养宠', '宠物多久驱虫一次？', '驱虫周期与宠物年龄、生活环境、外出频率和所用产品有关。常见做法是幼宠更频繁、成年宠按月或按季度管理，具体应参考兽医建议和产品说明。', '多久驱虫,驱虫周期,体内驱虫,体外驱虫', 310
where not exists (select 1 from ai_faq where question = '宠物多久驱虫一次？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '健康记录与科学养宠', '宠物体重突然变化怎么办？', '如果宠物体重短期明显下降或上升，应同时观察食欲、饮水、精神、排便、运动量等情况。若伴随呕吐、腹泻、精神差等症状，应尽快就医。', '体重变化,突然变瘦,突然变胖,体重下降,体重上升', 320
where not exists (select 1 from ai_faq where question = '宠物体重突然变化怎么办？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'CUSTOMER', '健康记录与科学养宠', '新宠到家要注意什么？', '新宠到家建议先提供安静环境，保持固定饮食和饮水，不要频繁洗澡或突然更换食物。观察精神、食欲和排便，逐步建立疫苗、驱虫和体重记录。', '新宠,到家,适应,新手养宠', 330
where not exists (select 1 from ai_faq where question = '新宠到家要注意什么？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ADMIN', '客户服务与投诉处理', '客户不满意服务效果怎么处理？', '先安抚客户情绪，确认具体不满意点，核对服务记录和交付照片或备注。能现场修正的尽量安排补救，无法立即处理的记录为客服工单，并约定反馈时间。', '投诉,不满意,服务效果,客诉,客服', 400
where not exists (select 1 from ai_faq where question = '客户不满意服务效果怎么处理？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ADMIN', '客户服务与投诉处理', '客户反复咨询订单进度怎么办？', '可先说明订单当前状态和下一步处理节点，并引导客户查看订单详情或 AI 咨询入口。高频问题应沉淀为 FAQ，减少重复人工回复。', '反复咨询,订单进度,客服,高频问题', 410
where not exists (select 1 from ai_faq where question = '客户反复咨询订单进度怎么办？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ADMIN', '管理端经营分析', '什么是优质客户？', '优质客户可从消费金额、复购次数、最近活跃时间、订单完成率、托管或高毛利项目偏好、投诉率等维度综合判断。建议优先维护高复购、高客单、低投诉且近期活跃的客户。', '优质客户,客户价值,高复购,高客单,客户维护', 500
where not exists (select 1 from ai_faq where question = '什么是优质客户？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ADMIN', '管理端经营分析', '如何识别优质服务项目？', '优质项目通常具备订单量稳定、利润率较高、客户复购好、服务耗时可控、投诉少等特征。不能只看营收，也要同时看成本、利润和交付难度。', '优质项目,服务项目,利润率,营收,成本', 510
where not exists (select 1 from ai_faq where question = '如何识别优质服务项目？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ADMIN', '财务状况与利润提升', '本月财务状况应该怎么看？', '建议先看总营收、总成本、净利润和利润率，再看服务项目利润排行、低利润项目、订单量变化和异常成本。若营收上升但利润率下降，需要重点检查成本或折扣。', '财务状况,本月财务,营收,成本,净利润,利润率', 600
where not exists (select 1 from ai_faq where question = '本月财务状况应该怎么看？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ADMIN', '财务状况与利润提升', '如何提高赚取利润的效率？', '可以优先推广高利润且交付稳定的项目，优化低利润项目的定价或成本，设计洗护加购和托管套餐，提升复购提醒效率，并减少无效预约和人工重复沟通。', '提高利润,利润效率,赚钱效率,套餐,复购,低利润', 610
where not exists (select 1 from ai_faq where question = '如何提高赚取利润的效率？');

insert into ai_faq (role_scope, category, question, answer, keywords, sort_order)
select 'ADMIN', '财务状况与利润提升', '哪些项目适合做套餐？', '适合做套餐的项目通常是高频、强关联、客户容易理解的组合，例如洗澡加基础护理、托管加洗护、疫苗提醒加健康记录管理。套餐定价要同时考虑客户感知价值和门店利润。', '套餐,项目组合,加购,洗护套餐,托管套餐', 620
where not exists (select 1 from ai_faq where question = '哪些项目适合做套餐？');

insert into pet (customer_id, name, species, breed, gender, weight, sterilized, remark)
select id, '豆豆', '狗', '柯基', 'MALE', 8.50, 1, '性格温顺'
from customer
where phone = '13800000001'
  and not exists (select 1 from pet where name = '豆豆');

insert into pet (customer_id, name, species, breed, gender, weight, sterilized, remark)
select id, '雪球', '猫', '英短', 'FEMALE', 4.20, 1, '怕生，护理时需要安抚'
from customer
where phone = '13800000002'
  and not exists (select 1 from pet where name = '雪球');

insert into service_item (name, category, price, cost, duration_minutes, description)
select '基础洗护', '洗护', 88.00, 32.00, 60, '洗澡、吹干、基础护理'
where not exists (select 1 from service_item where name = '基础洗护');

insert into service_item (name, category, price, cost, duration_minutes, description)
select '精致美容', '美容', 168.00, 76.00, 120, '修剪造型、洗护、基础护理'
where not exists (select 1 from service_item where name = '精致美容');

update service_item set cost = 32.00 where name = '基础洗护' and cost = 0;
update service_item set cost = 76.00 where name = '精致美容' and cost = 0;

create temporary table if not exists clinic_order_cleanup_ids (id integer primary key);
delete from clinic_order_cleanup_ids;
insert or ignore into clinic_order_cleanup_ids (id)
select distinct order_id
from service_order_item
where service_name = '门诊问诊'
   or service_item_id in (select id from service_item where name = '门诊问诊');
delete from payment_record
where order_type = 'SERVICE'
  and order_id in (select id from clinic_order_cleanup_ids);
delete from order_status_log
where order_id in (select id from clinic_order_cleanup_ids);
delete from service_order_item
where order_id in (select id from clinic_order_cleanup_ids);
delete from service_order
where id in (select id from clinic_order_cleanup_ids);
delete from service_item
where name = '门诊问诊';
drop table clinic_order_cleanup_ids;

insert into service_order (order_no, customer_id, pet_id, appointment_time, status, total_amount, total_cost, total_profit, remark)
select 'SO-DEMO-PENDING',
       c.id,
       p.id,
       datetime('now', '+1 day', 'localtime'),
       'PENDING',
       s.price,
       s.cost,
       s.price - s.cost,
       '演示待确认订单'
from customer c
join pet p on p.customer_id = c.id and p.name = '雪球'
join service_item s on s.name = '基础洗护'
where c.phone = '13800000002'
  and not exists (select 1 from service_order where order_no = 'SO-DEMO-PENDING');

update service_order
set total_cost = (select s.cost from service_item s where s.name = '基础洗护'),
    total_profit = total_amount - (select s.cost from service_item s where s.name = '基础洗护')
where order_no = 'SO-DEMO-PENDING'
  and total_cost = 0;

insert into service_order_item (order_id, service_item_id, service_name, unit_price, unit_cost, quantity, subtotal, cost_subtotal, profit)
select o.id, s.id, s.name, s.price, s.cost, 1, s.price, s.cost, s.price - s.cost
from service_order o
join service_item s on s.name = '基础洗护'
where o.order_no = 'SO-DEMO-PENDING'
  and not exists (select 1 from service_order_item where order_id = o.id and service_item_id = s.id);

update service_order_item
set unit_cost = (select s.cost from service_item s where s.id = service_order_item.service_item_id),
    cost_subtotal = (select s.cost from service_item s where s.id = service_order_item.service_item_id) * quantity,
    profit = subtotal - ((select s.cost from service_item s where s.id = service_order_item.service_item_id) * quantity)
where unit_cost = 0;

update service_order
set total_cost = coalesce((select sum(cost_subtotal) from service_order_item where order_id = service_order.id), 0),
    total_profit = total_amount - coalesce((select sum(cost_subtotal) from service_order_item where order_id = service_order.id), 0)
where exists (select 1 from service_order_item where order_id = service_order.id);

insert into order_status_log (order_id, old_status, new_status, operator, remark)
select o.id, null, 'PENDING', 'admin', '创建演示订单'
from service_order o
where o.order_no = 'SO-DEMO-PENDING'
  and not exists (select 1 from order_status_log where order_id = o.id and new_status = 'PENDING');

insert into vaccine_record (pet_id, vaccine_name, vaccination_date, institution, next_vaccination_date, remark)
select p.id, '犬六联疫苗', date('now', '-90 day', 'localtime'), '安心宠物医院', date('now', '+275 day', 'localtime'), '演示疫苗记录'
from pet p
where p.name = '豆豆'
  and not exists (select 1 from vaccine_record where pet_id = p.id and vaccine_name = '犬六联疫苗');

insert into deworming_record (pet_id, deworming_type, medicine_name, deworming_date, next_deworming_date, remark)
select p.id, '体内', '拜宠清', date('now', '-30 day', 'localtime'), date('now', '+60 day', 'localtime'), '演示驱虫记录'
from pet p
where p.name = '豆豆'
  and not exists (select 1 from deworming_record where pet_id = p.id and medicine_name = '拜宠清');

insert into weight_record (pet_id, record_date, weight, remark)
select p.id, date('now', '-7 day', 'localtime'), 8.60, '演示体重记录'
from pet p
where p.name = '豆豆'
  and not exists (select 1 from weight_record where pet_id = p.id and record_date = date('now', '-7 day', 'localtime'));

insert into boarding_area (name, sort_order, remark)
select '一楼东区', 10, '大型犬托管主区域'
where not exists (select 1 from boarding_area where name = '一楼东区');

insert into boarding_area (name, sort_order, remark)
select '一楼西区', 20, '中小型犬托管区域'
where not exists (select 1 from boarding_area where name = '一楼西区');

insert into boarding_area (name, sort_order, remark)
select '猫舍区', 30, '猫咪独立安静区域'
where not exists (select 1 from boarding_area where name = '猫舍区');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'A01', 'A01 大型房', '房间', '狗', '大型', 2, 'ENABLED', 'CLEAN', '靠近护理台，适合大型犬'
from boarding_area a
where a.name = '一楼东区'
  and not exists (select 1 from boarding_location where code = 'A01');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'A02', 'A02 大型房', '房间', '狗', '大型', 2, 'MAINTENANCE', 'DIRTY', '待清洁后恢复上架'
from boarding_area a
where a.name = '一楼东区'
  and not exists (select 1 from boarding_location where code = 'A02');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'B01', 'B01 小型笼位', '笼位', '狗', '小型', 1, 'ENABLED', 'CLEAN', '适合小型犬短期托管'
from boarding_area a
where a.name = '一楼西区'
  and not exists (select 1 from boarding_location where code = 'B01');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'C01', 'C01 猫咪套间', '套间', '猫', '通用', 1, 'ENABLED', 'CLEANING', '猫咪独立空间'
from boarding_area a
where a.name = '猫舍区'
  and not exists (select 1 from boarding_location where code = 'C01');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'A03', 'A03 大型房', '房间', '狗', '大型', 2, 'ENABLED', 'CLEAN', '大型犬标准托管房'
from boarding_area a
where a.name = '一楼东区'
  and not exists (select 1 from boarding_location where code = 'A03');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'A04', 'A04 大型房', '房间', '狗', '大型', 2, 'ENABLED', 'CLEAN', '大型犬标准托管房'
from boarding_area a
where a.name = '一楼东区'
  and not exists (select 1 from boarding_location where code = 'A04');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'A05', 'A05 大型房', '房间', '狗', '大型', 2, 'ENABLED', 'CLEAN', '大型犬标准托管房'
from boarding_area a
where a.name = '一楼东区'
  and not exists (select 1 from boarding_location where code = 'A05');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'B02', 'B02 小型笼位', '笼位', '狗', '小型', 1, 'ENABLED', 'CLEAN', '小型犬独立笼位'
from boarding_area a
where a.name = '一楼西区'
  and not exists (select 1 from boarding_location where code = 'B02');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'B03', 'B03 小型笼位', '笼位', '狗', '小型', 1, 'ENABLED', 'CLEAN', '小型犬独立笼位'
from boarding_area a
where a.name = '一楼西区'
  and not exists (select 1 from boarding_location where code = 'B03');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'B04', 'B04 中型笼位', '笼位', '狗', '中型', 1, 'ENABLED', 'CLEAN', '中型犬独立笼位'
from boarding_area a
where a.name = '一楼西区'
  and not exists (select 1 from boarding_location where code = 'B04');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'B05', 'B05 中型笼位', '笼位', '狗', '中型', 1, 'ENABLED', 'CLEAN', '中型犬独立笼位'
from boarding_area a
where a.name = '一楼西区'
  and not exists (select 1 from boarding_location where code = 'B05');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'C02', 'C02 猫咪套间', '套间', '猫', '通用', 1, 'ENABLED', 'CLEAN', '猫咪独立套间'
from boarding_area a
where a.name = '猫舍区'
  and not exists (select 1 from boarding_location where code = 'C02');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'C03', 'C03 猫咪套间', '套间', '猫', '通用', 1, 'ENABLED', 'CLEAN', '猫咪独立套间'
from boarding_area a
where a.name = '猫舍区'
  and not exists (select 1 from boarding_location where code = 'C03');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'C04', 'C04 猫咪套间', '套间', '猫', '通用', 1, 'ENABLED', 'CLEAN', '猫咪独立套间'
from boarding_area a
where a.name = '猫舍区'
  and not exists (select 1 from boarding_location where code = 'C04');

insert into boarding_location (area_id, code, name, location_type, pet_species, pet_size, capacity, status, clean_status, remark)
select a.id, 'C05', 'C05 猫咪套间', '套间', '猫', '通用', 1, 'ENABLED', 'CLEAN', '猫咪独立套间'
from boarding_area a
where a.name = '猫舍区'
  and not exists (select 1 from boarding_location where code = 'C05');

update boarding_area set name = '一楼东区', remark = '大型犬托管主区域' where id = 1;
update boarding_area set name = '一楼西区', remark = '中小型犬托管区域' where id = 2;
update boarding_area set name = '猫舍区', remark = '猫咪独立安静区域' where id = 3;

update boarding_location
set name = 'A01 大型房',
    location_type = '房间',
    pet_species = '狗',
    pet_size = '大型',
    remark = '靠近护理台，适合大型犬'
where code = 'A01';

update boarding_location
set name = 'A02 大型房',
    location_type = '房间',
    pet_species = '狗',
    pet_size = '大型',
    remark = '待清洁后恢复上架'
where code = 'A02';

update boarding_location
set name = 'B01 小型笼位',
    location_type = '笼位',
    pet_species = '狗',
    pet_size = '小型',
    remark = '适合小型犬短期托管'
where code = 'B01';

update boarding_location
set name = 'C01 猫咪套间',
    location_type = '套间',
    pet_species = '猫',
    pet_size = '通用',
    remark = '猫咪独立空间'
where code = 'C01';

update boarding_location
set price_per_day = 128.00,
    cost_per_day = 46.00
where code in ('A01', 'A02', 'A03', 'A04', 'A05');

update boarding_location
set price_per_day = 88.00,
    cost_per_day = 30.00
where code in ('B01', 'B02', 'B03', 'B04', 'B05');

update boarding_location
set price_per_day = 108.00,
    cost_per_day = 38.00
where code in ('C01', 'C02', 'C03', 'C04', 'C05');

insert into boarding_order (boarding_no, customer_id, pet_id, location_id, planned_check_in_time, planned_check_out_time, status, total_amount, remark)
select 'BO-DEMO-RESERVED-01',
       c.id,
       p.id,
       l.id,
       datetime('now', '+1 day', 'localtime'),
       datetime('now', '+3 day', 'localtime'),
       'RESERVED',
       198.00,
       '演示托管预约'
from customer c
join pet p on p.customer_id = c.id and p.name = '豆豆'
join boarding_location l on l.code = 'A01'
where c.phone = '13800000001'
  and not exists (select 1 from boarding_order where boarding_no = 'BO-DEMO-RESERVED-01');

insert into boarding_order (boarding_no, customer_id, pet_id, location_id, planned_check_in_time, planned_check_out_time, status, total_amount, remark)
select 'BO-DEMO-RESERVED-02',
       c.id,
       p.id,
       l.id,
       datetime('now', '+2 day', 'localtime'),
       datetime('now', '+5 day', 'localtime'),
       'RESERVED',
       268.00,
       '演示猫咪托管预约'
from customer c
join pet p on p.customer_id = c.id and p.name = '雪球'
join boarding_location l on l.code = 'C02'
where c.phone = '13800000002'
  and not exists (select 1 from boarding_order where boarding_no = 'BO-DEMO-RESERVED-02');

update boarding_order
set unit_price = (select price_per_day from boarding_location where boarding_location.id = boarding_order.location_id),
    unit_cost = (select cost_per_day from boarding_location where boarding_location.id = boarding_order.location_id),
    charge_days = max(1, cast((julianday(planned_check_out_time) - julianday(planned_check_in_time) + 0.999999) as integer)),
    total_amount = (select price_per_day from boarding_location where boarding_location.id = boarding_order.location_id)
        * max(1, cast((julianday(planned_check_out_time) - julianday(planned_check_in_time) + 0.999999) as integer)),
    total_cost = (select cost_per_day from boarding_location where boarding_location.id = boarding_order.location_id)
        * max(1, cast((julianday(planned_check_out_time) - julianday(planned_check_in_time) + 0.999999) as integer)),
    total_profit = ((select price_per_day from boarding_location where boarding_location.id = boarding_order.location_id)
        - (select cost_per_day from boarding_location where boarding_location.id = boarding_order.location_id))
        * max(1, cast((julianday(planned_check_out_time) - julianday(planned_check_in_time) + 0.999999) as integer))
where unit_price = 0;

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '柯基', '柯基,威尔士柯基,corgi', '/pet-avatars/system/corgi.png', 'SYSTEM', 10, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '柯基');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '金毛', '金毛,金毛寻回犬,golden retriever,golden-retriever', '/pet-avatars/system/golden-retriever.png', 'SYSTEM', 20, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '金毛');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '拉布拉多', '拉布拉多,labrador', '/pet-avatars/system/labrador.png', 'SYSTEM', 30, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '拉布拉多');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '贵宾', '贵宾,泰迪,poodle,teddy', '/pet-avatars/system/poodle.png', 'SYSTEM', 40, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '贵宾');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '博美', '博美,pomeranian', '/pet-avatars/system/pomeranian.png', 'SYSTEM', 50, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '博美');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '比熊', '比熊,bichon', '/pet-avatars/system/bichon.png', 'SYSTEM', 60, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '比熊');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '哈士奇', '哈士奇,husky,siberian husky', '/pet-avatars/system/husky.png', 'SYSTEM', 70, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '哈士奇');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '萨摩耶', '萨摩耶,samoyed', '/pet-avatars/system/samoyed.png', 'SYSTEM', 80, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '萨摩耶');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '柴犬', '柴犬,shiba,shiba inu', '/pet-avatars/system/shiba.png', 'SYSTEM', 90, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '柴犬');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '狗', '边牧', '边牧,边境牧羊犬,border collie,border-collie', '/pet-avatars/system/border-collie.png', 'SYSTEM', 100, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '边牧');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '英短', '英短,英国短毛,british shorthair,british-shorthair', '/pet-avatars/system/british-shorthair.png', 'SYSTEM', 110, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '英短');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '美短', '美短,美国短毛,american shorthair,american-shorthair', '/pet-avatars/system/american-shorthair.png', 'SYSTEM', 120, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '美短');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '布偶', '布偶,ragdoll', '/pet-avatars/system/ragdoll.png', 'SYSTEM', 130, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '布偶');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '暹罗', '暹罗,siamese', '/pet-avatars/system/siamese.png', 'SYSTEM', 140, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '暹罗');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '缅因', '缅因,maine coon,maine-coon', '/pet-avatars/system/maine-coon.png', 'SYSTEM', 150, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '缅因');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '加菲', '加菲,异国短毛,exotic shorthair,exotic-shorthair', '/pet-avatars/system/exotic-shorthair.png', 'SYSTEM', 160, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '加菲');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '橘猫', '橘猫,orange tabby,orange-tabby', '/pet-avatars/system/orange-tabby.png', 'SYSTEM', 170, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '橘猫');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '狸花', '狸花,中华田园猫,li hua,li-hua,tabby', '/pet-avatars/system/li-hua.png', 'SYSTEM', 180, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '狸花');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '银渐层', '银渐层,silver shaded,silver-shaded', '/pet-avatars/system/silver-shaded.png', 'SYSTEM', 190, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '银渐层');

insert into pet_avatar_library (species, breed, keywords, avatar_url, source_type, sort_order, remark)
select '猫', '金渐层', '金渐层,golden shaded,golden-shaded', '/pet-avatars/system/golden-shaded.png', 'SYSTEM', 200, '系统内置头像'
where not exists (select 1 from pet_avatar_library where breed = '金渐层');
