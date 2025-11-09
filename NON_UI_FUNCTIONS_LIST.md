# Danh sách các Non-UI Functions trong Project

## 1. SERVICE LAYER

### CartService
- `getCurrentUser()` - Lấy người dùng hiện tại từ Spring Security Context
- `getOrCreateMyCart(boolean fetchItems)` - Lấy hoặc tạo giỏ hàng cho người dùng
- `getOrCreateMyCart()` - Overload không cần fetch items
- `addProduct(Long productId, int quantity)` - Thêm sản phẩm vào giỏ hàng
- `updateQuantity(Long productId, int quantity)` - Cập nhật số lượng sản phẩm
- `removeProduct(Long productId)` - Xóa sản phẩm khỏi giỏ hàng
- `getMyCartWithItems()` - Lấy giỏ hàng với tất cả items
- `clearCart()` - Xóa tất cả sản phẩm khỏi giỏ hàng
- `getCartPaymentInfo()` - Lấy thông tin giỏ hàng để thanh toán

### OrderService
- `createOrderFromCart(Long buyerId, List<Map<String, Object>> cartItems, String paymentMethod, String notes)` - Tạo order từ cart
- `createOrderFromCart(Long buyerId, List<Map<String, Object>> cartItems, String paymentMethod, String notes, String customOrderCode)` - Tạo order với orderCode tùy chỉnh
- `createSimpleOrder(Long buyerId, Long sellerId, Long shopId, Long productId, Long productVariantId, Long warehouseId, Integer quantity, BigDecimal unitPrice, String paymentMethod, String notes)` - Tạo order đơn giản
- `createSimpleOrder(Long buyerId, Long sellerId, Long shopId, Long productId, Long productVariantId, Long warehouseId, Integer quantity, BigDecimal unitPrice, String paymentMethod, String notes, String customOrderCode)` - Tạo order đơn giản với orderCode
- `updateOrderStatus(Long orderId, Order.Status status)` - Cập nhật trạng thái order
- `updateOrderStatusByOrderCode(String orderCode, Order.Status status)` - Cập nhật trạng thái tất cả orders theo orderCode
- `getOrdersByBuyer(Long buyerId)` - Lấy danh sách orders của buyer
- `getOrdersByBuyerWithFilters(Long buyerId, String startDate, String endDate, String searchSeller, String searchProduct, String sortBy)` - Lấy orders với filter
- `getOrdersBySeller(Long sellerId)` - Lấy danh sách orders của seller
- `getOrderByCode(String orderCode)` - Lấy order theo order code
- `getOrderWithItems(Long orderId)` - Lấy order với chi tiết OrderItem
- `getCommissionRate(Long productId)` - Lấy commission rate từ shop
- `generateOrderCode()` - Tạo order code unique
- `getOrderStatsForSeller(Long sellerId)` - Tính toán thống kê cho seller
- `getOrderStatsForBuyer(Long buyerId)` - Tính toán thống kê cho buyer

### PaymentService
- `createPaymentUrl(PaymentRequest request)` - Tạo payment URL
- `createPaymentUrl(PaymentRequest request, HttpServletRequest httpRequest)` - Tạo payment URL với request
- `processPaymentCallback(String orderId, Long amount, String vnpTxnRef, String vnpTransactionNo)` - Xử lý callback thanh toán
- `handleFailedPayment(String orderId, Long amount, String vnpTxnRef, String vnpTransactionNo, String responseCode)` - Xử lý thanh toán thất bại
- `processCartPayment(CartPaymentRequest request)` - Xử lý thanh toán giỏ hàng
- `getPaymentStatus(Long paymentId)` - Lấy trạng thái payment
- `checkStockAvailability(List<Map<String, Object>> cartItems)` - Kiểm tra tình trạng tồn kho

### PaymentQueueService
- `enqueuePayment(Long userId, List<Map<String, Object>> cartItems, BigDecimal totalAmount)` - Thêm payment vào queue
- `validateStockAvailability(List<Map<String, Object>> cartItems)` - Validate stock trước khi enqueue
- `validateUserBalance(Long userId, BigDecimal requiredAmount)` - Validate số dư user
- `processPaymentQueue()` - Cron job xử lý payment queue (mỗi 1 giây)
- `processPaymentItem(PaymentQueue payment)` - Xử lý một payment item
- `createOrderWithItems(Long userId, List<Map<String, Object>> cartItems, List<Warehouse> lockedItems, String orderId)` - Tạo order với nhiều items
- `updateOrderItemsWithActualWarehouseIds(Order order, List<Warehouse> lockedItems)` - Cập nhật warehouseId cho OrderItems
- `handlePaymentError(PaymentQueue payment, String orderId, String errorMessage)` - Xử lý lỗi payment
- `handlePaymentErrorWithoutHold(PaymentQueue payment, String errorMessage)` - Xử lý lỗi không có hold
- `markPaymentAsFailed(Long paymentId, String errorMessage)` - Đánh dấu payment thất bại
- `parseCartData(String cartDataJson)` - Parse cart data từ JSON
- `getPaymentStatus(Long paymentId)` - Lấy trạng thái payment
- `getUserPayments(Long userId)` - Lấy danh sách payments của user
- `processBatchPayments(List<PaymentQueue> pendingPayments)` - Xử lý batch payments
- `processSingleBatch(List<PaymentQueue> batch)` - Xử lý một batch payments

### WarehouseLockService
- `lockWarehouseItem(Long productId)` - Lock một warehouse item
- `lockWarehouseItems(List<Long> productIds)` - Lock nhiều warehouse items
- `lockWarehouseItemsWithQuantities(Map<Long, Integer> productQuantities)` - Lock warehouse items theo số lượng
- `unlockWarehouseItem(Long warehouseId)` - Unlock warehouse item
- `unlockWarehouseItems(List<Long> warehouseIds)` - Unlock nhiều warehouse items
- `markAsDelivered(Long warehouseId)` - Đánh dấu warehouse item đã giao
- `getCurrentUserId()` - Lấy current user ID
- `getAvailableStockCount(Long productId)` - Lấy số lượng hàng có sẵn cho product
- `getAvailableStockCountByVariant(Long productVariantId)` - Lấy số lượng hàng có sẵn cho productVariant
- `reserveWarehouseItemsWithTimeoutByVariant(Map<Long, Integer> productVariantQuantities, Long userId, int timeoutMinutes)` - Reserve warehouse items với timeout (theo variant)
- `reserveWarehouseItemsWithTimeout(Map<Long, Integer> productQuantities, Long userId, int timeoutMinutes)` - Reserve warehouse items với timeout
- `releaseExpiredReservations()` - Cron job release expired reservations (mỗi 1 phút)

### UserService
- `register(UserCreateRequest request)` - Đăng ký user mới với status PENDING
- `verify(String email, String otp, String ipAddress)` - Xác thực OTP và kích hoạt user
- `forgotPassword(String email)` - Gửi OTP để quên mật khẩu
- `getProfile(String username)` - Lấy thông tin profile
- `updateProfile(String username, ProfileUpdateRequest request)` - Cập nhật profile
- `deleteProfile(String username)` - Xóa profile
- `getUserByUsername(String username)` - Lấy user theo username
- `changePassword(String username, String currentPassword, String newPassword)` - Đổi mật khẩu (theo username)
- `changePassword(Long userId, String currentPassword, String newPassword)` - Đổi mật khẩu (theo userId)
- `sendForgotPasswordOtp(String email)` - Gửi OTP quên mật khẩu
- `verifyForgotPasswordOtp(String email, String otp, String ipAddress)` - Xác thực OTP quên mật khẩu
- `resetPassword(String email, String resetToken, String newPassword)` - Reset mật khẩu
- `resetPasswordWithToken(String email, String resetToken, String newPassword)` - Reset mật khẩu với token
- `updateProfile(Long userId, UpdateProfileRequest request)` - Cập nhật profile (theo userId)
- `sendOTPAsync(String email, String otp, String purpose)` - Gửi OTP async
- `sendOTPWithHtmlAsync(String email, String otp, String purpose)` - Gửi OTP HTML async
- `uploadAvatar(Long userId, MultipartFile file)` - Upload avatar
- `deleteAvatar(Long userId)` - Xóa avatar
- `convertToUserCreateRequest(Object rawData)` - Convert raw data sang UserCreateRequest
- `convertToString(Object value)` - Convert object sang string
- `findById(Long id)` - Tìm user theo ID
- `save(User user)` - Lưu user

### ProductService
- (Cần đọc file để liệt kê đầy đủ)

### ShopService
- (Cần đọc file để liệt kê đầy đủ)

### SellerService
- (Cần đọc file để liệt kê đầy đủ)

### ReviewService
- (Cần đọc file để liệt kê đầy đủ)

### AuthenticationService
- (Cần đọc file để liệt kê đầy đủ)

### JwtService
- (Cần đọc file để liệt kê đầy đủ)

### OtpService
- `sendOtp(String email, String purpose)` - Gửi OTP qua email
- `verifyOtp(String email, String otp, String purpose, String ipAddress)` - Xác thực OTP
- `generateResetToken(String email)` - Tạo reset token sau khi verify OTP
- `markOtpVerified(String email, String purpose)` - Đánh dấu OTP đã được verify
- `isOtpVerified(String email, String purpose)` - Kiểm tra OTP đã được verify chưa
- `clearOtpVerificationState(String email, String purpose)` - Xóa trạng thái verify OTP
- `validateResetToken(String resetToken, String email)` - Validate reset token
- `invalidateResetToken(String resetToken, String email)` - Vô hiệu hóa reset token
- `generateOtp()` - Tạo OTP 6 số
- `isOtpValid(String email, String purpose)` - Kiểm tra OTP còn hợp lệ không
- `getRemainingAttempts(String email, String purpose)` - Lấy số lần thử còn lại
- `convertToOtpData(Object rawData)` - Convert raw data sang OtpData

### EmailService
- (Cần đọc file để liệt kê đầy đủ)

### EmailTemplateService
- (Cần đọc file để liệt kê đầy đủ)

### WalletHistoryService
- `saveDepositHistory(Long walletId, BigDecimal amount, String vnpTxnRef, String vnpTransactionNo)` - Lưu lịch sử nạp tiền
- `saveHistory(Long walletId, BigDecimal amount, String vnpTxnRef, String vnpTransactionNo, WalletHistory.Type type, WalletHistory.Status status, String description)` - Lưu lịch sử ví
- `getWalletHistoryByWalletId(Long walletId)` - Lấy lịch sử ví theo walletId
- `existsByTransactionNoAndTypeAndStatus(String transactionNo, WalletHistory.Type type, WalletHistory.Status status)` - Kiểm tra transaction đã xử lý chưa
- `existsByReferenceIdAndTypeAndStatus(String referenceId, WalletHistory.Type type, WalletHistory.Status status)` - Kiểm tra order đã xử lý chưa
- `updateWalletHistoryStatus(Long walletId, String referenceId, WalletHistory.Type type, WalletHistory.Status newStatus, String description)` - Cập nhật trạng thái lịch sử ví

### WalletHoldService
- `holdMoney(Long userId, BigDecimal amount, String orderId)` - Hold tiền trong ví với timeout 1 phút
- `releaseHold(Long holdId)` - Release hold money về ví
- `releaseHold(Long userId, String orderId)` - Release hold money theo userId và orderId
- `completeHold(Long holdId)` - Complete hold - chuyển tiền cho seller và admin
- `processExpiredHolds()` - Cron job xử lý expired holds (mỗi 5 giây)
- `distributePaymentToSellerAndAdmin(WalletHold hold, List<Order> orders)` - Chuyển tiền cho seller và admin theo commission
- `getActiveHolds(Long userId)` - Lấy danh sách holds đang active
- `getTotalHeldAmount(Long userId)` - Lấy tổng số tiền đang bị hold
- `processBatchExpiredHolds(List<WalletHold> expiredHolds)` - Xử lý batch expired holds
- `processSingleBatchExpiredHolds(List<WalletHold> batch)` - Xử lý một batch expired holds

### WithdrawService
- `createWithdrawRequest(WithdrawRequestDto requestDto)` - Tạo yêu cầu rút tiền
- `getWithdrawRequestsByUser()` - Lấy danh sách yêu cầu rút tiền của user
- `getWithdrawRequestsByUserWithFilters(...)` - Lấy yêu cầu rút tiền với filters
- `getAllPendingWithdrawRequests()` - Lấy tất cả yêu cầu rút tiền đang pending (admin)
- `approveWithdrawRequest(Long requestId)` - Duyệt yêu cầu rút tiền (admin)
- `rejectWithdrawRequest(Long requestId)` - Từ chối yêu cầu rút tiền (admin)
- `getAllPendingWithdrawRequestsSimple()` - Lấy tất cả yêu cầu rút tiền đang pending (simple)
- `approveWithdrawRequestSimple(Long requestId)` - Duyệt yêu cầu rút tiền (simple)
- `rejectWithdrawRequestSimple(Long requestId)` - Từ chối yêu cầu rút tiền (simple)
- `getWithdrawRequestsByStatus(WithdrawRequest.Status status)` - Lấy yêu cầu rút tiền theo status
- `filterWithdrawRequests(...)` - Filter yêu cầu rút tiền
- `cancelWithdrawRequest(Long requestId)` - Hủy yêu cầu rút tiền và hoàn tiền

### AuditLogService
- (Cần đọc file để liệt kê đầy đủ)

### ApiCallLogService
- (Cần đọc file để liệt kê đầy đủ)

### UserActivityLogService
- (Cần đọc file để liệt kê đầy đủ)

### SecurityEventService
- (Cần đọc file để liệt kê đầy đủ)

### LoginAttemptService
- (Cần đọc file để liệt kê đầy đủ)

### IpLockoutService
- (Cần đọc file để liệt kê đầy đủ)

### OtpLockoutService
- (Cần đọc file để liệt kê đầy đủ)

### CaptchaService
- (Cần đọc file để liệt kê đầy đủ)

### CaptchaRateLimitService
- (Cần đọc file để liệt kê đầy đủ)

### RateLimitService
- (Cần đọc file để liệt kê đầy đủ)

### ResetTokenLockoutService
- (Cần đọc file để liệt kê đầy đủ)

### InventoryReservationService
- (Cần đọc file để liệt kê đầy đủ)

### PaymentTriggerService
- (Cần đọc file để liệt kê đầy đủ)

### PerformanceMonitoringService
- (Cần đọc file để liệt kê đầy đủ)

### LogCleanupScheduler
- (Cần đọc file để liệt kê đầy đủ)

### UserDetailServiceCustomizer
- (Cần đọc file để liệt kê đầy đủ)

### CustomOAuth2UserService
- (Cần đọc file để liệt kê đầy đủ)

## 2. REPOSITORY LAYER

### CartRepository
- (Các methods từ JpaRepository + custom queries)

### CartItemRepository
- (Các methods từ JpaRepository + custom queries)

### OrderRepository
- (Các methods từ JpaRepository + custom queries)

### OrderItemRepository
- (Các methods từ JpaRepository + custom queries)

### ProductRepository
- (Các methods từ JpaRepository + custom queries)

### ProductVariantRepository
- (Các methods từ JpaRepository + custom queries)

### WarehouseRepository
- `findFirstByProductIdAndLockedFalseAndIsDeleteFalse(Long productId)` - Tìm warehouse item đầu tiên
- `findFirstByProductVariantIdAndLockedFalseAndIsDeleteFalse(Long productVariantId)` - Tìm warehouse item đầu tiên theo variant
- `findByProductIdAndLockedFalseAndIsDeleteFalseOrderByCreatedAtAsc(Long productId, Pageable pageable)` - Tìm warehouse items với pagination
- `countByProductIdAndLockedFalseAndIsDeleteFalse(Long productId)` - Đếm số lượng warehouse items
- `countByProductVariantIdAndLockedFalseAndIsDeleteFalse(Long productVariantId)` - Đếm số lượng warehouse items theo variant
- `findAvailableItemsForReservation(Long productId, Integer quantity)` - Tìm items để reserve (SELECT FOR UPDATE)
- `findAvailableItemsForReservationByVariant(Long productVariantId, Integer quantity)` - Tìm items để reserve theo variant
- `findByLockedTrueAndReservedUntilBefore(LocalDateTime now)` - Tìm expired reservations
- (Các methods từ JpaRepository + custom queries khác)

### UserRepository
- (Các methods từ JpaRepository + custom queries)

### ShopRepository
- (Các methods từ JpaRepository + custom queries)

### WalletRepository
- (Các methods từ JpaRepository + custom queries)

### WalletHistoryRepository
- (Các methods từ JpaRepository + custom queries)

### WalletHoldRepository
- (Các methods từ JpaRepository + custom queries)

### WithdrawRequestRepository
- (Các methods từ JpaRepository + custom queries)

### ReviewRepository
- (Các methods từ JpaRepository + custom queries)

### CategoryRepository
- (Các methods từ JpaRepository + custom queries)

### PaymentQueueRepository
- (Các methods từ JpaRepository + custom queries)

### PaymentLogRepository
- (Các methods từ JpaRepository + custom queries)

### AuditLogRepository
- (Các methods từ JpaRepository + custom queries)

### ApiCallLogRepository
- (Các methods từ JpaRepository + custom queries)

### UserActivityLogRepository
- (Các methods từ JpaRepository + custom queries)

### SecurityEventRepository
- (Các methods từ JpaRepository + custom queries)

### IpLockoutRepository
- (Các methods từ JpaRepository + custom queries)

### RedisTokenRepository
- (Các methods từ JpaRepository + custom queries)

### UploadHistoryRepository
- (Các methods từ JpaRepository + custom queries)

## 3. UTILITY LAYER

### VNPayUtil
- `createPaymentUrl(long amount, String orderInfo, String orderId)` - Tạo payment URL
- `createPaymentUrl(long amount, String orderInfo, String orderId, HttpServletRequest request)` - Tạo payment URL với request
- `hmacSHA512(String key, String data)` - Tạo HMAC SHA512 hash
- `getClientIpAddress(HttpServletRequest request)` - Lấy client IP address
- `sortObject(Map<String, String> obj)` - Sort parameters và build hash data string
- `verifyPayment(Map<String, String> params)` - Verify payment signature

### StatusUtils
- `getStatusDisplayName(Order.Status status)` - Lấy tên hiển thị của Order status
- `getStatusDisplayName(OrderItem.Status status)` - Lấy tên hiển thị của OrderItem status

### PaginationValidator
- `validatePage(int page)` - Validate và sanitize page number
- `validateOneBasedPage(int pageOneBased)` - Validate 1-based page
- `toZeroBased(int pageOneBased)` - Convert 1-based page sang zero-based
- `validateSize(int size)` - Validate và sanitize page size
- `validatePageAgainstTotal(int page, int totalPages)` - Validate page number với total pages
- `clampOneBased(int pageOneBased, int totalPages)` - Clamp 1-based page trong range
- `getDefaultPage()` - Lấy default page number
- `getDefaultSize()` - Lấy default page size
- `getMinPage()` - Lấy minimum page number
- `getMinSize()` - Lấy minimum page size
- `getMaxSize()` - Lấy maximum page size

### RequestMetadataUtil
- `extractClientIp(HttpServletRequest request)` - Extract client IP từ request
- `extractUserAgent(HttpServletRequest request)` - Extract User-Agent từ request

## 4. EVENT LAYER

### PaymentEventListener
- (Các event handlers)

### WalletHoldEventListener
- (Các event handlers)

## 5. ASPECT LAYER

### UserActivityAspect
- (AOP methods cho user activity logging)

## 6. FILTER/INTERCEPTOR LAYER

### ApiCallLogFilter
- (Filter methods)

### CaptchaValidationFilter
- (Filter methods)

### ShopLockInterceptor
- (Interceptor methods)

### JwtAuthenticationFilter
- (Filter methods)

### IpBlockingFilter
- (Filter methods)

## 7. CONFIGURATION LAYER

### SecurityConfiguration
- (Configuration methods)

### RedisConfiguration
- (Configuration methods)

### VNPayConfig
- (Configuration methods)

### WebConfig
- (Configuration methods)

### AsyncConfig
- (Configuration methods)

### AuditAsyncConfig
- (Configuration methods)

### KaptchaConfig
- (Configuration methods)

### JwtDecoderConfiguration
- (Configuration methods)

### ApplicationConfig
- (Configuration methods)

### FilterConfig
- (Configuration methods)

## 8. INITIALIZATION LAYER

### DataInitializer
- (Initialization methods)

### DatabaseMigrationRunner
- (Migration methods)

## Lưu ý:
- Danh sách này chưa đầy đủ 100% vì một số service classes chưa được đọc chi tiết
- Các Repository methods thường kế thừa từ JpaRepository nên có sẵn các methods như: save, findById, findAll, delete, etc.
- Một số methods có thể là private/protected và chỉ được sử dụng nội bộ trong class

