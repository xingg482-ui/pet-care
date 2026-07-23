create table if not exists customer (
    id integer primary key autoincrement,
    name text not null,
    phone text not null unique,
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
    status text not null default 'ENABLED',
    remark text,
    created_at text not null default (datetime('now', 'localtime')),
    updated_at text not null default (datetime('now', 'localtime')),
    foreign key (customer_id) references customer(id)
);

create table if not exists service_item (
    id integer primary key autoincrement,
    name text not null,
    category text not null,
    price numeric not null,
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
    quantity integer not null default 1,
    subtotal numeric not null,
    foreign key (order_id) references service_order(id),
    foreign key (service_item_id) references service_item(id)
);

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
