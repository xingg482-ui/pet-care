package com.example.petcare.account;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petcare.common.PageResult;
import com.example.petcare.customer.Customer;
import com.example.petcare.customer.CustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AccountService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ADMIN = "ADMIN";
    private static final String CUSTOMER = "CUSTOMER";
    private static final String ACTIVE = "ACTIVE";
    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String REJECTED = "REJECTED";
    private static final String DISABLED = "DISABLED";
    private static final String APPROVE = "APPROVE";
    private static final String REJECT = "REJECT";
    private static final String ENABLE = "ENABLE";
    private static final String DISABLE = "DISABLE";
    private static final String TOKEN_PREFIX = "pet-care-token-";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern PHONE_ACCOUNT_PATTERN = Pattern.compile("^1\\d{10}$");
    private static final Pattern EMAIL_ACCOUNT_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AccountMapper accountMapper;
    private final AccountAuditLogMapper accountAuditLogMapper;
    private final CustomerMapper customerMapper;
    private final PasswordHasher passwordHasher;

    public AccountService(
            AccountMapper accountMapper,
            AccountAuditLogMapper accountAuditLogMapper,
            CustomerMapper customerMapper,
            PasswordHasher passwordHasher
    ) {
        this.accountMapper = accountMapper;
        this.accountAuditLogMapper = accountAuditLogMapper;
        this.customerMapper = customerMapper;
        this.passwordHasher = passwordHasher;
    }

    public AuthResult login(LoginRequest request) {
        Account account = findByUsername(request.username());
        if (account == null || !passwordHasher.matches(request.password(), account.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        ensureCanLogin(account);
        return toAuthResult(account, TOKEN_PREFIX + account.getId() + "-" + UUID.randomUUID());
    }

    public boolean isUsernameAvailable(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        String normalized = normalizeUsername(username);
        if (!isPhoneAccount(normalized) && !isEmailAccount(normalized)) {
            return false;
        }
        return isUsernameUnique(normalized);
    }

    @Transactional
    public RegisterResult register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        ensureRegisterAccountFormat(username);
        ensureUsernameUnique(username);
        String role = normalizeRole(request.role());
        String now = now();

        Account account = new Account();
        account.setUsername(username);
        account.setDisplayName(request.displayName().trim());
        account.setPasswordHash(passwordHasher.hash(request.password()));
        account.setRole(role);
        account.setStatus(CUSTOMER.equals(role) ? ACTIVE : PENDING_REVIEW);
        account.setPhone(resolveRegisterPhone(username, request.phone()));
        account.setAvatarUrl(trimToNull(request.avatarUrl()));
        account.setCreatedAt(now);
        account.setUpdatedAt(now);

        if (CUSTOMER.equals(role)) {
            Customer customer = createCustomerForAccount(account, username, now);
            account.setCustomerId(customer.getId());
        }

        accountMapper.insert(account);
        String message = CUSTOMER.equals(role)
                ? "注册成功，可直接登录"
                : "注册成功，等待高级管理员审核通过后可登录";
        return new RegisterResult(account.getId(), account.getUsername(), account.getDisplayName(), account.getRole(), account.getStatus(), message);
    }

    public AccountPrincipal me(String authorization) {
        Account account = accountFromAuthorization(authorization);
        ensureCanLogin(account);
        return new AccountPrincipal(account.getId(), account.getUsername(), account.getDisplayName(), account.getRole(), account.getAvatarUrl(), account.getCustomerId());
    }

    public AccountView profile(String authorization) {
        Account account = accountFromAuthorization(authorization);
        ensureCanLogin(account);
        return AccountView.from(account);
    }

    public AccountPrincipal requireStaff(String authorization) {
        Account account = accountFromAuthorization(authorization);
        ensureCanLogin(account);
        if (!SUPER_ADMIN.equals(account.getRole()) && !ADMIN.equals(account.getRole())) {
            throw new IllegalArgumentException("无权限访问客服消息");
        }
        return new AccountPrincipal(account.getId(), account.getUsername(), account.getDisplayName(), account.getRole(), account.getAvatarUrl(), account.getCustomerId());
    }

    public AccountPrincipal requireCustomer(String authorization) {
        Account account = accountFromAuthorization(authorization);
        ensureCanLogin(account);
        if (!CUSTOMER.equals(account.getRole()) || account.getCustomerId() == null) {
            throw new IllegalArgumentException("无权限访问客户自助功能");
        }
        return new AccountPrincipal(account.getId(), account.getUsername(), account.getDisplayName(), account.getRole(), account.getAvatarUrl(), account.getCustomerId());
    }

    @Transactional
    public AccountView updateProfile(ProfileUpdateRequest request, String authorization) {
        Account account = accountFromAuthorization(authorization);
        ensureCanLogin(account);
        String displayName = request.displayName().trim();
        String phone = trimToNull(request.phone());
        String avatarUrl = trimToNull(request.avatarUrl());
        String now = now();

        if (CUSTOMER.equals(account.getRole()) && phone != null) {
            ensureCustomerPhoneUnique(phone, account.getCustomerId());
        }

        accountMapper.update(new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, account.getId())
                .set(Account::getDisplayName, displayName)
                .set(Account::getPhone, phone)
                .set(Account::getAvatarUrl, avatarUrl)
                .set(Account::getUpdatedAt, now));

        if (CUSTOMER.equals(account.getRole()) && account.getCustomerId() != null) {
            customerMapper.update(new LambdaUpdateWrapper<Customer>()
                    .eq(Customer::getId, account.getCustomerId())
                    .set(Customer::getName, displayName)
                    .set(Customer::getPhone, phone)
                    .set(Customer::getUpdatedAt, now));
        }

        return AccountView.from(getByIdOrThrow(account.getId()));
    }

    @Transactional
    public AccountView restoreDefaultAvatar(String authorization) {
        Account account = accountFromAuthorization(authorization);
        ensureCanLogin(account);
        accountMapper.update(new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, account.getId())
                .set(Account::getAvatarUrl, null)
                .set(Account::getUpdatedAt, now()));
        return AccountView.from(getByIdOrThrow(account.getId()));
    }

    @Transactional
    public void changePassword(PasswordChangeRequest request, String authorization) {
        Account account = accountFromAuthorization(authorization);
        ensureCanLogin(account);
        if (request.newPassword().length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        if (!passwordHasher.matches(request.oldPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码错误");
        }
        accountMapper.update(new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, account.getId())
                .set(Account::getPasswordHash, passwordHasher.hash(request.newPassword()))
                .set(Account::getUpdatedAt, now()));
    }

    public PageResult<AccountView> list(AccountQuery query, String authorization) {
        requireSuperAdmin(authorization);
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .like(StringUtils.hasText(query.getUsername()), Account::getUsername, query.getUsername())
                .like(StringUtils.hasText(query.getDisplayName()), Account::getDisplayName, query.getDisplayName())
                .eq(StringUtils.hasText(query.getRole()), Account::getRole, query.getRole())
                .eq(StringUtils.hasText(query.getStatus()), Account::getStatus, query.getStatus())
                .orderByDesc(Account::getCreatedAt);
        Page<Account> page = accountMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        List<AccountView> records = page.getRecords().stream().map(AccountView::from).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public AccountView detail(Long id, String authorization) {
        requireSuperAdmin(authorization);
        return AccountView.from(getByIdOrThrow(id));
    }

    @Transactional
    public AccountView approve(Long id, String authorization) {
        Account operator = requireSuperAdmin(authorization);
        Account account = getByIdOrThrow(id);
        if (!PENDING_REVIEW.equals(account.getStatus())) {
            throw new IllegalArgumentException("只有待审核账号可以审核通过");
        }
        String oldStatus = account.getStatus();
        String now = now();
        accountMapper.update(new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, id)
                .set(Account::getStatus, ACTIVE)
                .set(Account::getReviewedBy, operator.getId())
                .set(Account::getReviewedAt, now)
                .set(Account::getRejectReason, null)
                .set(Account::getUpdatedAt, now));
        insertAuditLog(id, APPROVE, oldStatus, ACTIVE, operator.getId(), null, now);
        return AccountView.from(getByIdOrThrow(id));
    }

    @Transactional
    public AccountView reject(Long id, AccountRejectRequest request, String authorization) {
        Account operator = requireSuperAdmin(authorization);
        Account account = getByIdOrThrow(id);
        if (!PENDING_REVIEW.equals(account.getStatus())) {
            throw new IllegalArgumentException("只有待审核账号可以审核拒绝");
        }
        String oldStatus = account.getStatus();
        String now = now();
        String reason = request.reason().trim();
        accountMapper.update(new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, id)
                .set(Account::getStatus, REJECTED)
                .set(Account::getReviewedBy, operator.getId())
                .set(Account::getReviewedAt, now)
                .set(Account::getRejectReason, reason)
                .set(Account::getUpdatedAt, now));
        insertAuditLog(id, REJECT, oldStatus, REJECTED, operator.getId(), reason, now);
        return AccountView.from(getByIdOrThrow(id));
    }

    @Transactional
    public AccountView updateStatus(Long id, AccountStatusRequest request, String authorization) {
        Account operator = requireSuperAdmin(authorization);
        Account account = getByIdOrThrow(id);
        String nextStatus = normalizeAccountStatus(request.status());
        if (!ACTIVE.equals(nextStatus) && !DISABLED.equals(nextStatus)) {
            throw new IllegalArgumentException("账号启停状态不合法");
        }
        if (operator.getId().equals(id) && DISABLED.equals(nextStatus)) {
            throw new IllegalArgumentException("不能停用自己的账号");
        }
        if (SUPER_ADMIN.equals(account.getRole()) && DISABLED.equals(nextStatus)) {
            ensureAnotherActiveSuperAdmin(id);
        }
        if (nextStatus.equals(account.getStatus())) {
            return AccountView.from(account);
        }
        String action = ACTIVE.equals(nextStatus) ? ENABLE : DISABLE;
        String now = now();
        String reason = trimToNull(request.reason());
        accountMapper.update(new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, id)
                .set(Account::getStatus, nextStatus)
                .set(Account::getUpdatedAt, now));
        insertAuditLog(id, action, account.getStatus(), nextStatus, operator.getId(), reason, now);
        return AccountView.from(getByIdOrThrow(id));
    }

    private Customer createCustomerForAccount(Account account, String username, String now) {
        String phone = trimToNull(account.getPhone());
        if (phone != null && customerMapper.selectCount(new LambdaQueryWrapper<Customer>().eq(Customer::getPhone, phone)) > 0) {
            throw new IllegalArgumentException("手机号已存在");
        }
        Customer customer = new Customer();
        customer.setName(account.getDisplayName());
        customer.setPhone(phone);
        customer.setEmail(isEmailAccount(username) ? username : null);
        customer.setStatus("ENABLED");
        customer.setRemark("客户账号注册自动创建");
        customer.setCreatedAt(now);
        customer.setUpdatedAt(now);
        customerMapper.insert(customer);
        return customer;
    }

    private Account accountFromAuthorization(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer " + TOKEN_PREFIX)) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
        String tokenBody = authorization.substring(("Bearer " + TOKEN_PREFIX).length());
        int separator = tokenBody.indexOf('-');
        if (separator <= 0) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
        try {
            Long accountId = Long.valueOf(tokenBody.substring(0, separator));
            Account account = accountMapper.selectById(accountId);
            if (account == null) {
                throw new IllegalArgumentException("登录状态已失效，请重新登录");
            }
            return account;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录");
        }
    }

    private Account requireSuperAdmin(String authorization) {
        Account account = accountFromAuthorization(authorization);
        ensureCanLogin(account);
        if (!SUPER_ADMIN.equals(account.getRole())) {
            throw new IllegalArgumentException("无权限访问账号管理");
        }
        return account;
    }

    private Account getByIdOrThrow(Long id) {
        Account account = accountMapper.selectById(id);
        if (account == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        return account;
    }

    private void ensureAnotherActiveSuperAdmin(Long currentId) {
        Long count = accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getRole, SUPER_ADMIN)
                .eq(Account::getStatus, ACTIVE)
                .ne(Account::getId, currentId));
        if (count <= 0) {
            throw new IllegalArgumentException("系统必须保留至少一个正常状态的高级管理员");
        }
    }

    private void insertAuditLog(Long accountId, String action, String oldStatus, String newStatus, Long operatorId, String reason, String now) {
        AccountAuditLog log = new AccountAuditLog();
        log.setAccountId(accountId);
        log.setAction(action);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperatorId(operatorId);
        log.setReason(reason);
        log.setCreatedAt(now);
        accountAuditLogMapper.insert(log);
    }

    private Account findByUsername(String username) {
        return accountMapper.selectOne(new LambdaQueryWrapper<Account>().eq(Account::getUsername, normalizeUsername(username)));
    }

    private AuthResult toAuthResult(Account account, String token) {
        return new AuthResult(
                token,
                account.getId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRole(),
                account.getStatus(),
                account.getAvatarUrl(),
                account.getCustomerId()
        );
    }

    private void ensureCanLogin(Account account) {
        if (PENDING_REVIEW.equals(account.getStatus())) {
            throw new IllegalArgumentException("账号待高级管理员审核通过后可登录");
        }
        if (REJECTED.equals(account.getStatus())) {
            throw new IllegalArgumentException("账号审核未通过，请联系高级管理员");
        }
        if (DISABLED.equals(account.getStatus())) {
            throw new IllegalArgumentException("账号已停用，请联系高级管理员");
        }
        if (!ACTIVE.equals(account.getStatus())) {
            throw new IllegalArgumentException("账号状态异常，请联系高级管理员");
        }
    }

    private void ensureUsernameUnique(String username) {
        if (!isUsernameUnique(username)) {
            throw new IllegalArgumentException("登录账号已存在");
        }
    }

    private boolean isUsernameUnique(String username) {
        return accountMapper.selectCount(new LambdaQueryWrapper<Account>().eq(Account::getUsername, username)) == 0;
    }

    private void ensureRegisterAccountFormat(String username) {
        if (!isPhoneAccount(username) && !isEmailAccount(username)) {
            throw new IllegalArgumentException("请使用手机号或邮箱注册");
        }
    }

    private void ensureCustomerPhoneUnique(String phone, Long customerId) {
        if (customerMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPhone, phone)
                .ne(customerId != null, Customer::getId, customerId)) > 0) {
            throw new IllegalArgumentException("手机号已存在");
        }
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!SUPER_ADMIN.equals(normalized) && !ADMIN.equals(normalized) && !CUSTOMER.equals(normalized)) {
            throw new IllegalArgumentException("登录角色不合法");
        }
        return normalized;
    }

    private String normalizeAccountStatus(String status) {
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!ACTIVE.equals(normalized) && !DISABLED.equals(normalized) && !PENDING_REVIEW.equals(normalized) && !REJECTED.equals(normalized)) {
            throw new IllegalArgumentException("账号状态不合法");
        }
        return normalized;
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveRegisterPhone(String username, String phone) {
        String explicitPhone = trimToNull(phone);
        if (explicitPhone != null) {
            return explicitPhone;
        }
        return isPhoneAccount(username) ? username : null;
    }

    private boolean isPhoneAccount(String username) {
        return PHONE_ACCOUNT_PATTERN.matcher(username).matches();
    }

    private boolean isEmailAccount(String username) {
        return EMAIL_ACCOUNT_PATTERN.matcher(username).matches();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String now() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
