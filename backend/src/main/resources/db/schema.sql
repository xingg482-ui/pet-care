create table if not exists account (
    id integer primary key autoincrement,
    username text not null unique,
    display_name text not null,
    password_hash text not null,
    role text not null,
    status text not null default 'PENDING_REVIEW',
    phone text,
    avatar_url text,
    customer_id integer,
    reviewed_by integer,
    reviewed_at text,
    reject_reason text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (customer_id) references customer(id),
    foreign key (reviewed_by) references account(id)
);

create index if not exists idx_account_role on account(role);
create index if not exists idx_account_status on account(status);
create index if not exists idx_account_customer on account(customer_id);

create table if not exists account_audit_log (
    id integer primary key autoincrement,
    account_id integer not null,
    action text not null,
    old_status text,
    new_status text not null,
    operator_id integer not null,
    reason text,
    created_at text not null default (datetime('now', 'localtime')),
    foreign key (account_id) references account(id),
    foreign key (operator_id) references account(id)
);

create index if not exists idx_account_audit_log_account on account_audit_log(account_id);
create index if not exists idx_account_audit_log_operator on account_audit_log(operator_id);

create table if not exists support_ticket (
    id integer primary key autoincrement,
    ticket_no text not null unique,
    contact_name text not null,
    contact_info text not null,
    username text,
    issue_type text not null,
    content text not null,
    status text not null default 'OPEN',
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime'))
);

create index if not exists idx_support_ticket_no on support_ticket(ticket_no);
create index if not exists idx_support_ticket_status on support_ticket(status);
create index if not exists idx_support_ticket_contact on support_ticket(contact_info);

create table if not exists support_ticket_reply (
    id integer primary key autoincrement,
    ticket_id integer not null,
    replier_account_id integer not null,
    replier_role text not null,
    content text not null,
    created_at text not null default (datetime('now', 'localtime')),
    foreign key (ticket_id) references support_ticket(id),
    foreign key (replier_account_id) references account(id)
);

create index if not exists idx_support_ticket_reply_ticket on support_ticket_reply(ticket_id);

create table if not exists ai_faq (
    id integer primary key autoincrement,
    role_scope text not null,
    category text not null,
    question text not null,
    answer text not null,
    keywords text,
    enabled integer not null default 1,
    sort_order integer not null default 0,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime'))
);

create index if not exists idx_ai_faq_role_scope on ai_faq(role_scope);
create index if not exists idx_ai_faq_category on ai_faq(category);
create index if not exists idx_ai_faq_enabled on ai_faq(enabled);

create table if not exists ai_chat_session (
    id integer primary key autoincrement,
    account_id integer not null,
    role_scope text not null,
    title text,
    source_page text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (account_id) references account(id)
);

create index if not exists idx_ai_chat_session_account on ai_chat_session(account_id);
create index if not exists idx_ai_chat_session_updated_at on ai_chat_session(updated_at);

create table if not exists ai_chat_message (
    id integer primary key autoincrement,
    session_id integer not null,
    sender text not null,
    content text not null,
    source text,
    risk_level text,
    created_at text not null default (datetime('now', 'localtime')),
    foreign key (session_id) references ai_chat_session(id)
);

create index if not exists idx_ai_chat_message_session on ai_chat_message(session_id);
create index if not exists idx_ai_chat_message_created_at on ai_chat_message(created_at);

create table if not exists customer (
    id integer primary key autoincrement,
    name text not null,
    phone text unique,
    email text,
    address text,
    status text not null default 'ENABLED',
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime'))
);

create table if not exists pet (
    id integer primary key autoincrement,
    customer_id integer not null,
    name text not null,
    species text not null,
    breed text,
    gender text not null default 'UNKNOWN',
    birthday text,
    weight numeric,
    sterilized integer,
    color text,
    microchip_no text,
    allergies text,
    medical_history text,
    diet_preference text,
    behavior_notes text,
    exercise_level text,
    care_notes text,
    avatar_url text,
    avatar_source text,
    status text not null default 'ENABLED',
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (customer_id) references customer(id)
);

alter table pet add column avatar_url text;
alter table pet add column avatar_source text;
alter table pet add column color text;
alter table pet add column microchip_no text;
alter table pet add column allergies text;
alter table pet add column medical_history text;
alter table pet add column diet_preference text;
alter table pet add column behavior_notes text;
alter table pet add column exercise_level text;
alter table pet add column care_notes text;

create table if not exists pet_avatar_library (
    id integer primary key autoincrement,
    species text not null,
    breed text not null,
    keywords text,
    avatar_url text not null,
    source_type text not null default 'SYSTEM',
    sort_order integer not null default 0,
    status text not null default 'ENABLED',
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime'))
);

create index if not exists idx_pet_avatar_library_species on pet_avatar_library(species);
create index if not exists idx_pet_avatar_library_status on pet_avatar_library(status);

create table if not exists service_item (
    id integer primary key autoincrement,
    name text not null,
    category text not null,
    price numeric not null,
    cost numeric not null default 0,
    duration_minutes integer not null,
    status text not null default 'ENABLED',
    description text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime'))
);

create table if not exists service_order (
    id integer primary key autoincrement,
    order_no text not null unique,
    customer_id integer not null,
    pet_id integer not null,
    appointment_time text not null,
    status text not null default 'PENDING',
    total_amount numeric not null default 0,
    total_cost numeric not null default 0,
    total_profit numeric not null default 0,
    payment_status text not null default 'UNPAID',
    paid_amount numeric not null default 0,
    paid_at text,
    payment_method text,
    payment_no text,
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (customer_id) references customer(id),
    foreign key (pet_id) references pet(id)
);

create table if not exists service_order_item (
    id integer primary key autoincrement,
    order_id integer not null,
    service_item_id integer not null,
    service_name text not null,
    unit_price numeric not null,
    unit_cost numeric not null default 0,
    quantity integer not null default 1,
    subtotal numeric not null,
    cost_subtotal numeric not null default 0,
    profit numeric not null default 0,
    foreign key (order_id) references service_order(id),
    foreign key (service_item_id) references service_item(id)
);

alter table service_item add column cost numeric not null default 0;
alter table service_order add column total_cost numeric not null default 0;
alter table service_order add column total_profit numeric not null default 0;
alter table service_order add column payment_status text not null default 'UNPAID';
alter table service_order add column paid_amount numeric not null default 0;
alter table service_order add column paid_at text;
alter table service_order add column payment_method text;
alter table service_order add column payment_no text;
alter table service_order_item add column unit_cost numeric not null default 0;
alter table service_order_item add column cost_subtotal numeric not null default 0;
alter table service_order_item add column profit numeric not null default 0;

create index if not exists idx_service_order_payment_status on service_order(payment_status);
create unique index if not exists uk_service_order_payment_no on service_order(payment_no) where payment_no is not null;

create table if not exists payment_record (
    id integer primary key autoincrement,
    order_type text not null,
    order_id integer not null,
    payment_no text not null unique,
    amount numeric not null,
    payment_method text not null,
    payment_status text not null,
    paid_by_account_id integer not null,
    paid_at text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime'))
);

create index if not exists idx_payment_record_order on payment_record(order_type, order_id);
create index if not exists idx_payment_record_status on payment_record(payment_status);

insert or ignore into payment_record (
    order_type,
    order_id,
    payment_no,
    amount,
    payment_method,
    payment_status,
    paid_by_account_id,
    paid_at,
    created_at,
    updated_at
)
select 'SERVICE',
       id,
       payment_no,
       paid_amount,
       coalesce(payment_method, 'MOCK'),
       payment_status,
       0,
       paid_at,
       coalesce(paid_at, updated_at),
       coalesce(paid_at, updated_at)
from service_order
where payment_status = 'PAID'
  and payment_no is not null;

create table if not exists order_status_log (
    id integer primary key autoincrement,
    order_id integer not null,
    old_status text,
    new_status text not null,
    operator text not null default 'admin',
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    foreign key (order_id) references service_order(id)
);

create table if not exists vaccine_record (
    id integer primary key autoincrement,
    pet_id integer not null,
    vaccine_name text not null,
    vaccination_date text not null,
    institution text,
    next_vaccination_date text,
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (pet_id) references pet(id)
);

create table if not exists deworming_record (
    id integer primary key autoincrement,
    pet_id integer not null,
    deworming_type text not null,
    medicine_name text not null,
    deworming_date text not null,
    next_deworming_date text,
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (pet_id) references pet(id)
);

create table if not exists weight_record (
    id integer primary key autoincrement,
    pet_id integer not null,
    record_date text not null,
    weight numeric not null,
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (pet_id) references pet(id)
);

create table if not exists boarding_area (
    id integer primary key autoincrement,
    name text not null,
    sort_order integer not null default 0,
    status text not null default 'ENABLED',
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime'))
);

create table if not exists boarding_location (
    id integer primary key autoincrement,
    area_id integer not null,
    code text not null unique,
    name text not null,
    location_type text not null,
    pet_species text not null,
    pet_size text not null,
    capacity integer not null default 1,
    price_per_day numeric not null default 0,
    cost_per_day numeric not null default 0,
    status text not null default 'ENABLED',
    clean_status text not null default 'CLEAN',
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (area_id) references boarding_area(id)
);

create index if not exists idx_boarding_location_area on boarding_location(area_id);
create index if not exists idx_boarding_location_status on boarding_location(status);
create index if not exists idx_boarding_location_clean_status on boarding_location(clean_status);

create table if not exists boarding_order (
    id integer primary key autoincrement,
    boarding_no text not null unique,
    customer_id integer not null,
    pet_id integer not null,
    location_id integer not null,
    planned_check_in_time text not null,
    planned_check_out_time text not null,
    actual_check_in_time text,
    actual_check_out_time text,
    status text not null default 'RESERVED',
    unit_price numeric not null default 0,
    unit_cost numeric not null default 0,
    charge_days integer not null default 1,
    total_amount numeric not null default 0,
    total_cost numeric not null default 0,
    total_profit numeric not null default 0,
    payment_status text not null default 'UNPAID',
    paid_amount numeric not null default 0,
    paid_at text,
    payment_method text,
    payment_no text,
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (customer_id) references customer(id),
    foreign key (pet_id) references pet(id),
    foreign key (location_id) references boarding_location(id)
);

create index if not exists idx_boarding_order_location_time on boarding_order(location_id, planned_check_in_time, planned_check_out_time);
create index if not exists idx_boarding_order_pet_time on boarding_order(pet_id, planned_check_in_time, planned_check_out_time);
create index if not exists idx_boarding_order_status on boarding_order(status);

alter table boarding_location add column price_per_day numeric not null default 0;
alter table boarding_location add column cost_per_day numeric not null default 0;
alter table boarding_order add column unit_price numeric not null default 0;
alter table boarding_order add column unit_cost numeric not null default 0;
alter table boarding_order add column charge_days integer not null default 1;
alter table boarding_order add column total_cost numeric not null default 0;
alter table boarding_order add column total_profit numeric not null default 0;
alter table boarding_order add column payment_status text not null default 'UNPAID';
alter table boarding_order add column paid_amount numeric not null default 0;
alter table boarding_order add column paid_at text;
alter table boarding_order add column payment_method text;
alter table boarding_order add column payment_no text;
create index if not exists idx_boarding_order_payment_status on boarding_order(payment_status);
create unique index if not exists uk_boarding_order_payment_no on boarding_order(payment_no) where payment_no is not null;

create table if not exists boarding_care_task (
    id integer primary key autoincrement,
    boarding_order_id integer not null,
    task_type text not null,
    task_name text not null,
    task_date text not null,
    task_time text not null,
    status text not null default 'PENDING',
    remark text,
    completed_at text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (boarding_order_id) references boarding_order(id)
);

drop index if exists uk_boarding_care_task_daily;
create unique index if not exists uk_boarding_care_task_daily_time on boarding_care_task(boarding_order_id, task_date, task_type, task_time);
create index if not exists idx_boarding_care_task_date_status on boarding_care_task(task_date, status);
