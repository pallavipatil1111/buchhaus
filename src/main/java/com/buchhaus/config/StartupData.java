package com.buchhaus.config;

import com.buchhaus.core.OrderStatus;
import com.buchhaus.domain.Role;
import com.buchhaus.domain.User;
import com.buchhaus.repository.UserRepository;
import com.buchhaus.service.CartService;
import com.buchhaus.service.OrderService;
import com.buchhaus.web.dto.CheckoutRequest;
import com.buchhaus.web.dto.OrderView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates the admin account on first start (credentials from ADMIN_EMAIL / ADMIN_PASSWORD) and,
 * when DEMO_DATA=true, a demo customer with a few orders placed through the real checkout code so
 * the admin dashboard has something to show.
 */
@Configuration
public class StartupData {

    public static final String DEMO_EMAIL = "reader@buchhaus.local";
    public static final String DEMO_PASSWORD = "reader1234";

    private static final Logger log = LoggerFactory.getLogger(StartupData.class);

    @Bean
    public CommandLineRunner seedAccounts(UserRepository users, PasswordEncoder encoder, CartService cart,
                                          OrderService orders,
                                          @Value("${app.admin.email}") String adminEmail,
                                          @Value("${app.admin.password}") String adminPassword,
                                          @Value("${app.demo-data:false}") boolean demoData) {
        return args -> {
            if (!users.existsByEmailIgnoreCase(adminEmail)) {
                users.save(new User("Store admin", adminEmail.toLowerCase(), encoder.encode(adminPassword), Role.ADMIN));
                log.info("Created admin account {}", adminEmail);
            }
            if (demoData && !users.existsByEmailIgnoreCase(DEMO_EMAIL)) {
                User reader = users.save(new User("Demo Reader", DEMO_EMAIL, encoder.encode(DEMO_PASSWORD), Role.CUSTOMER));
                CheckoutRequest address = new CheckoutRequest("Demo Reader", "9876543210",
                        "Flat 12, Shanti Apartments, Baner Road", "Pune", "411045");
                long[][] baskets = {{13, 1, 15, 1}, {19, 2, 20, 1}, {1, 1, 6, 1, 11, 1}};
                OrderStatus[] statuses = {OrderStatus.DELIVERED, OrderStatus.SHIPPED, OrderStatus.PLACED};
                for (int i = 0; i < baskets.length; i++) {
                    for (int j = 0; j < baskets[i].length; j += 2) {
                        cart.setQuantity(reader.getId(), baskets[i][j], (int) baskets[i][j + 1]);
                    }
                    OrderView order = orders.checkout(reader.getId(), address);
                    if (statuses[i] != OrderStatus.PLACED) {
                        orders.updateStatus(order.id(), OrderStatus.SHIPPED);
                    }
                    if (statuses[i] == OrderStatus.DELIVERED) {
                        orders.updateStatus(order.id(), OrderStatus.DELIVERED);
                    }
                }
                log.info("Demo customer ready: {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
            }
        };
    }
}
