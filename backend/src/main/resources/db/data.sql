insert into customer (name, phone, email, address, remark)
select '张三', '13800000001', 'zhangsan@example.com', '示例小区 1 号', '演示客户'
where not exists (select 1 from customer where phone = '13800000001');

insert into customer (name, phone, email, address, remark)
select '李四', '13800000002', 'lisi@example.com', '演示花园 2 号', '演示客户'
where not exists (select 1 from customer where phone = '13800000002');

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

insert into service_item (name, category, price, duration_minutes, description)
select '基础洗护', '洗护', 88.00, 60, '洗澡、吹干、基础护理'
where not exists (select 1 from service_item where name = '基础洗护');

insert into service_item (name, category, price, duration_minutes, description)
select '门诊问诊', '问诊', 60.00, 30, '基础健康咨询'
where not exists (select 1 from service_item where name = '门诊问诊');

insert into service_item (name, category, price, duration_minutes, description)
select '精致美容', '美容', 168.00, 120, '修剪造型、洗护、基础护理'
where not exists (select 1 from service_item where name = '精致美容');

insert into service_order (order_no, customer_id, pet_id, appointment_time, status, total_amount, remark)
select 'SO-DEMO-PENDING',
       c.id,
       p.id,
       datetime('now', '+1 day', 'localtime'),
       'PENDING',
       s.price,
       '演示待确认订单'
from customer c
join pet p on p.customer_id = c.id and p.name = '雪球'
join service_item s on s.name = '基础洗护'
where c.phone = '13800000002'
  and not exists (select 1 from service_order where order_no = 'SO-DEMO-PENDING');

insert into service_order_item (order_id, service_item_id, service_name, unit_price, quantity, subtotal)
select o.id, s.id, s.name, s.price, 1, s.price
from service_order o
join service_item s on s.name = '基础洗护'
where o.order_no = 'SO-DEMO-PENDING'
  and not exists (select 1 from service_order_item where order_id = o.id and service_item_id = s.id);

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
