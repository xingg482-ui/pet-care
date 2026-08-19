package com.example.petcare.common;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private final DataSource dataSource;

    public DatabaseMigrationRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            migrateCustomerPhoneToNullable(connection);
            cleanGeneratedCustomerPhones(connection);
            ensureAiTables(connection);
        }
    }

    private void migrateCustomerPhoneToNullable(Connection connection) throws SQLException {
        if (!isCustomerPhoneNotNull(connection)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("pragma foreign_keys=off");
            statement.execute("pragma legacy_alter_table=on");
            statement.execute("alter table customer rename to customer_legacy_phone_not_null");
            statement.execute("""
                    create table customer (
                        id integer primary key autoincrement,
                        name text not null,
                        phone text unique,
                        email text,
                        address text,
                        status text not null default 'ENABLED',
                        remark text,
                        created_at text not null default (datetime('now', 'localtime')),
                        updated_at text not null default (datetime('now', 'localtime'))
                    )
                    """);
            statement.execute("""
                    insert into customer (id, name, phone, email, address, status, remark, created_at, updated_at)
                    select id,
                           name,
                           case when phone like 'ACCOUNT_%' then null else phone end,
                           email,
                           address,
                           status,
                           remark,
                           created_at,
                           updated_at
                    from customer_legacy_phone_not_null
                    """);
            statement.execute("drop table customer_legacy_phone_not_null");
            statement.execute("pragma legacy_alter_table=off");
            statement.execute("pragma foreign_keys=on");
        }
    }

    private boolean isCustomerPhoneNotNull(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("pragma table_info(customer)")) {
            while (resultSet.next()) {
                if ("phone".equalsIgnoreCase(resultSet.getString("name"))) {
                    return resultSet.getInt("notnull") == 1;
                }
            }
        }
        return false;
    }

    private void cleanGeneratedCustomerPhones(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("update customer set phone = null where phone like 'ACCOUNT_%'");
        }
    }

    private void ensureAiTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
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
                    )
                    """);
            statement.execute("create index if not exists idx_ai_faq_role_scope on ai_faq(role_scope)");
            statement.execute("create index if not exists idx_ai_faq_category on ai_faq(category)");
            statement.execute("create index if not exists idx_ai_faq_enabled on ai_faq(enabled)");
            statement.execute("""
                    create table if not exists ai_chat_session (
                        id integer primary key autoincrement,
                        account_id integer not null,
                        role_scope text not null,
                        title text,
                        source_page text,
                        created_at text not null default (datetime('now', 'localtime')),
                        updated_at text not null default (datetime('now', 'localtime')),
                        foreign key (account_id) references account(id)
                    )
                    """);
            statement.execute("create index if not exists idx_ai_chat_session_account on ai_chat_session(account_id)");
            statement.execute("create index if not exists idx_ai_chat_session_updated_at on ai_chat_session(updated_at)");
            statement.execute("""
                    create table if not exists ai_chat_message (
                        id integer primary key autoincrement,
                        session_id integer not null,
                        sender text not null,
                        content text not null,
                        source text,
                        risk_level text,
                        created_at text not null default (datetime('now', 'localtime')),
                        foreign key (session_id) references ai_chat_session(id)
                    )
                    """);
            statement.execute("create index if not exists idx_ai_chat_message_session on ai_chat_message(session_id)");
            statement.execute("create index if not exists idx_ai_chat_message_created_at on ai_chat_message(created_at)");
        }
    }
}
