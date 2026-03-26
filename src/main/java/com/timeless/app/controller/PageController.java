package com.timeless.app.controller;

import com.timeless.app.dto.response.CartItemResponse;
import com.timeless.app.dto.response.OrderResponse;
// import com.timeless.app.dto.response.ReviewResponse;
import com.timeless.app.dto.response.WatchCardResponse;
import com.timeless.app.dto.response.WatchResponse;
import com.timeless.app.dto.response.WishlistItemResponse;
import com.timeless.app.entity.Role;
import com.timeless.app.entity.WatchStatus;
import com.timeless.app.repository.PaymentRepository;
import com.timeless.app.repository.UserAccountRepository;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.service.CartService;
import com.timeless.app.service.OrderService;
// import com.timeless.app.service.ReviewService;
import com.timeless.app.service.WatchService;
import com.timeless.app.service.WishlistService;
import com.timeless.app.util.SecurityUtils;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PageController {

    private static final List<String> BRAND_OPTIONS = List.of(
            "Rolex", "Omega", "Patek Philippe", "Audemars Piguet", "Cartier", "TAG Heuer", "IWC", "Breitling", "Longines", "Seiko", "Other"
    );
    private static final List<String> CATEGORY_OPTIONS = List.of(
            "Dress", "Sport", "Dive", "Pilot", "Chronograph", "Luxury", "Vintage", "Other"
    );
    private static final List<String> CONDITION_OPTIONS = List.of("NEW", "LIKE_NEW", "GOOD", "FAIR");

    private final WatchService watchService;
//     private final ReviewService reviewService;
    private final CartService cartService;
    private final WishlistService wishlistService;
    private final OrderService orderService;
    private final UserAccountRepository userAccountRepository;
    private final PaymentRepository paymentRepository;

    @ModelAttribute
    public void populateCommonModel(Model model) {
        Optional<UserPrincipal> currentUser = SecurityUtils.getCurrentUserOptional();
        model.addAttribute("currentUser", currentUser.orElse(null));
        model.addAttribute("isAuthenticated", currentUser.isPresent());
        model.addAttribute("currentRole", currentUser.map(user -> user.getRole().name()).orElse(null));
        if (currentUser.isPresent() && currentUser.get().getRole() == Role.BUYER) {
            model.addAttribute("cartCount", cartService.countCartItems(currentUser.get().getId()));
            model.addAttribute("wishlistCount", wishlistService.countWishlistItems(currentUser.get().getId()));
        } else {
            model.addAttribute("cartCount", 0L);
            model.addAttribute("wishlistCount", 0L);
        }
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("featuredWatches", watchService.getFeaturedWatches(4));
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/watches")
    public String watchesPage(
            @RequestParam(required = false) List<String> brands,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<String> conditions,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model
    ) {
        Pageable pageable = resolvePageable(sort, page, size);
        Page<WatchResponse> watches = watchService.getActiveWatches(brands, categories, conditions, minPrice, maxPrice, search, pageable);
        if ("rating".equalsIgnoreCase(sort)) {
            List<WatchResponse> sorted = watches.getContent().stream()
                    .sorted(Comparator.comparingDouble(WatchResponse::getAverageRating).reversed())
                    .toList();
            watches = new PageImpl<>(sorted, pageable, watches.getTotalElements());
        }

        model.addAttribute("watchPage", watches);
        model.addAttribute("brandOptions", BRAND_OPTIONS);
        model.addAttribute("categoryOptions", CATEGORY_OPTIONS);
        model.addAttribute("conditionOptions", CONDITION_OPTIONS);
        model.addAttribute("selectedBrands", brands == null ? List.of() : brands);
        model.addAttribute("selectedCategories", categories == null ? List.of() : categories);
        model.addAttribute("selectedConditions", conditions == null ? List.of() : conditions);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("filterQueryString", buildFilterQueryString(brands, categories, conditions, minPrice, maxPrice, search, sort, size));
        return "watches";
    }

    @GetMapping("/watches/{id}")
    public String watchDetailPage(@PathVariable Long id, Model model) {
        WatchResponse watch = watchService.getWatchById(id);
//         List<ReviewResponse> reviews = reviewService.getReviewsForWatch(id);
        model.addAttribute("watch", watch);
//         model.addAttribute("reviews", reviews);
//
//         Optional<UserPrincipal> currentUser = SecurityUtils.getCurrentUserOptional();
//         boolean canReview = false;
//         Long reviewOrderId = null;
//         if (currentUser.isPresent() && currentUser.get().getRole() == Role.BUYER) {
//             Optional<com.timeless.app.entity.Order> reviewableOrder = orderService.findReviewableOrderForBuyerAndWatch(currentUser.get().getId(), id);
//             boolean alreadyReviewed = reviewService.hasBuyerReviewedWatch(currentUser.get().getId(), id);
//             canReview = reviewableOrder.isPresent() && !alreadyReviewed;
//             reviewOrderId = reviewableOrder.map(com.timeless.app.entity.Order::getId).orElse(null);
//         }
//         model.addAttribute("canReview", canReview);
//         model.addAttribute("reviewOrderId", reviewOrderId);
        model.addAttribute("reviews", List.of());
        model.addAttribute("canReview", false);
        model.addAttribute("reviewOrderId", null);
        return "watch-detail";
    }

    @GetMapping("/buyer/cart")
    @PreAuthorize("hasRole('BUYER')")
    public String buyerCartPage(Model model) {
        UserPrincipal buyer = SecurityUtils.getCurrentUser();
        List<CartItemResponse> cartItems = cartService.getCart(buyer.getId());
        BigDecimal total = cartItems.stream().map(CartItemResponse::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", total);
        return "buyer/cart";
    }

    @GetMapping("/buyer/payment")
    @PreAuthorize("hasRole('BUYER')")
    public String buyerPaymentPage(Model model) {
        UserPrincipal buyer = SecurityUtils.getCurrentUser();
        List<CartItemResponse> cartItems = cartService.getCart(buyer.getId());
        if (cartItems.isEmpty()) return "redirect:/buyer/cart";
        BigDecimal total = cartItems.stream().map(CartItemResponse::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", total);
        model.addAttribute("paymentMethods",
                java.util.Arrays.asList("BKASH", "NAGAD", "ROCKET", "CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER"));
        return "buyer/payment";
    }

    @GetMapping("/buyer/wishlist")
    @PreAuthorize("hasRole('BUYER')")
    public String buyerWishlistPage(Model model) {
        UserPrincipal buyer = SecurityUtils.getCurrentUser();
        model.addAttribute("wishlistItems", wishlistService.getWishlist(buyer.getId()));
        return "buyer/wishlist";
    }

    @GetMapping("/buyer/orders")
    @PreAuthorize("hasRole('BUYER')")
    public String buyerOrdersPage(Model model) {
        UserPrincipal buyer = SecurityUtils.getCurrentUser();
        model.addAttribute("orders", orderService.getMyOrdersAsBuyer(buyer.getId()));
        return "buyer/orders";
    }

    @GetMapping("/buyer/orders/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public String buyerOrderDetailPage(@PathVariable Long id, Model model) {
        UserPrincipal buyer = SecurityUtils.getCurrentUser();
        OrderResponse order = orderService.getOrderById(id, buyer.getId(), buyer.getRole());
//         boolean alreadyReviewed = reviewService.hasBuyerReviewedWatch(buyer.getId(), order.getWatchId());
//         boolean canReview = "COMPLETED".equals(order.getStatus()) && !alreadyReviewed;
        model.addAttribute("order", order);
        model.addAttribute("watch", watchService.getWatchById(order.getWatchId()));
//         model.addAttribute("canReview", canReview);
        model.addAttribute("canReview", false);
        return "buyer/order-detail";
    }

    @GetMapping("/seller/dashboard")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerDashboardPage(Model model) {
        UserPrincipal seller = SecurityUtils.getCurrentUser();
        List<WatchResponse> watches = watchService.getWatchesBySeller(seller.getId());
        List<OrderResponse> orders = orderService.getOrdersForSeller(seller.getId());
        BigDecimal revenue = orders.stream()
                .filter(order -> !"CANCELLED".equals(order.getStatus()))
                .map(OrderResponse::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("watches", watches);
        model.addAttribute("orders", orders);
        model.addAttribute("totalListings", watches.size());
        model.addAttribute("activeListings", watches.stream().filter(w -> "ACTIVE".equals(w.getStatus())).count());
        model.addAttribute("pendingListings", watches.stream().filter(w -> "PENDING_REVIEW".equals(w.getStatus())).count());
        model.addAttribute("totalOrders", orders.size());
        model.addAttribute("revenue", revenue);
        return "seller/dashboard";
    }

    @GetMapping("/seller/watches/create")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerCreateWatchPage(Model model) {
        model.addAttribute("brandOptions", BRAND_OPTIONS);
        model.addAttribute("categoryOptions", CATEGORY_OPTIONS);
        model.addAttribute("conditionOptions", CONDITION_OPTIONS);
        return "seller/create-watch";
    }

    @GetMapping("/seller/watches/{id}/edit")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerEditWatchPage(@PathVariable Long id, Model model) {
        UserPrincipal seller = SecurityUtils.getCurrentUser();
        WatchResponse watch = watchService.getWatchById(id);
        if (!watch.getSellerId().equals(seller.getId())) {
            throw new com.timeless.app.exception.ForbiddenException("You can only edit your own watch");
        }
        model.addAttribute("watch", watch);
        model.addAttribute("brandOptions", BRAND_OPTIONS);
        model.addAttribute("categoryOptions", CATEGORY_OPTIONS);
        model.addAttribute("conditionOptions", CONDITION_OPTIONS);
        return "seller/edit-watch";
    }

    @GetMapping("/seller/orders")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerOrdersPage(Model model) {
        UserPrincipal seller = SecurityUtils.getCurrentUser();
        model.addAttribute("orders", orderService.getOrdersForSeller(seller.getId()));
        return "seller/orders";
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboardPage(Model model) {
        model.addAttribute("stats", buildAdminStats());
        List<OrderResponse> recentOrders = orderService.getAllOrdersAdmin().stream().limit(10).toList();
        model.addAttribute("recentOrders", recentOrders);
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminUsersPage(@RequestParam(required = false) String role, Model model) {
        List<UserResponseView> users = (role == null || role.isBlank() ? userAccountRepository.findAll() : userAccountRepository.findByRole(Role.valueOf(role.toUpperCase(Locale.ROOT))))
                .stream()
                .sorted(Comparator.comparing(com.timeless.app.entity.UserAccount::getCreatedAt).reversed())
                .map(UserResponseView::from)
                .toList();
        model.addAttribute("users", users);
        model.addAttribute("selectedRole", role == null ? "" : role);
        return "admin/users";
    }

    @GetMapping("/admin/listings")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminListingsPage(@RequestParam(defaultValue = "ALL") String status, Model model) {
        List<WatchResponse> listings;
        if ("ALL".equalsIgnoreCase(status)) {
            listings = watchService.getAllWatchesForAdmin();
        } else {
            listings = watchService.getWatchesByStatus(WatchStatus.valueOf(status.toUpperCase(Locale.ROOT)));
        }
        model.addAttribute("listings", listings);
        model.addAttribute("selectedStatus", status.toUpperCase(Locale.ROOT));
        return "admin/listings";
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrdersPage(Model model) {
        model.addAttribute("orders", orderService.getAllOrdersAdmin());
        return "admin/orders";
    }

    private Pageable resolvePageable(String sort, int page, int size) {
        return switch (sort == null ? "newest" : sort.toLowerCase(Locale.ROOT)) {
            case "priceasc", "price-asc" -> PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "price"));
            case "pricedesc", "price-desc" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "price"));
            case "rating" -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        };
    }

    private String buildFilterQueryString(
            List<String> brands,
            List<String> categories,
            List<String> conditions,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            String sort,
            int size
    ) {
        StringBuilder builder = new StringBuilder();
        appendList(builder, "brands", brands);
        appendList(builder, "categories", categories);
        appendList(builder, "conditions", conditions);
        appendValue(builder, "minPrice", minPrice == null ? null : minPrice.toPlainString());
        appendValue(builder, "maxPrice", maxPrice == null ? null : maxPrice.toPlainString());
        appendValue(builder, "search", search);
        appendValue(builder, "sort", sort);
        appendValue(builder, "size", String.valueOf(size));
        return builder.toString();
    }

    private void appendList(StringBuilder builder, String key, List<String> values) {
        if (values == null) {
            return;
        }
        values.forEach(value -> appendValue(builder, key, value));
    }

    private void appendValue(StringBuilder builder, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append('&');
        }
        builder.append(key)
                .append('=')
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private Map<String, Object> buildAdminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        BigDecimal totalRevenue = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == com.timeless.app.entity.PaymentStatus.COMPLETED)
                .map(payment -> payment.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalUsers", userAccountRepository.count());
        stats.put("totalListings", watchService.getAllWatchesForAdmin().size());
        stats.put("pendingReview", watchService.getPendingWatches().size());
        stats.put("totalOrders", orderService.getAllOrdersAdmin().size());
        stats.put("totalRevenue", totalRevenue);
        return stats;
    }

    public record UserResponseView(Long id, String email, String fullName, String role, boolean enabled, java.time.LocalDateTime createdAt) {
        public static UserResponseView from(com.timeless.app.entity.UserAccount user) {
            return new UserResponseView(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name(), user.isEnabled(), user.getCreatedAt());
        }
    }
}
