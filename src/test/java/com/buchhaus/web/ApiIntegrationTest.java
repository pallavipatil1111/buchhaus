package com.buchhaus.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Through HTTP, security, validation, JPA (with row locks) and Flyway on H2 or MySQL. */
@SpringBootTest(properties = "app.demo-data=false")
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final Map<String, String> ADDRESS = Map.of("name", "Pallavi Patil", "phone", "9876543210",
            "address", "Flat 12, Baner Road", "city", "Pune", "pincode", "411045");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private String customer() throws Exception {
        String email = "c-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "Customer", "email", email, "password", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
        return login(email, "secret123");
    }

    private String admin() throws Exception {
        return login("admin@buchhaus.local", "Admin@12345");
    }

    private JsonNode setQty(String auth, long bookId, int qty) throws Exception {
        return json.readTree(mvc.perform(put("/api/cart/items/" + bookId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":" + qty + "}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long createBook(String sku, int stock) throws Exception {
        String body = mvc.perform(post("/api/admin/books").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("sku", sku, "title", "Test Book " + sku,
                                "author", "Tester", "genre", "Testing", "price", "300.00", "stock", stock))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    @Test
    void catalogueIsPublicAndSearchable() throws Exception {
        mvc.perform(get("/api/books").param("genre", "Classics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/books").param("q", "kalam"))
                .andExpect(jsonPath("$.content[0].title").value("Wings of Fire"));
        mvc.perform(get("/api/books").param("genre", "Technology").param("sort", "price_desc"))
                .andExpect(jsonPath("$.content[0].title").value("Introduction to Algorithms"))
                .andExpect(jsonPath("$.totalElements").value(6));
        mvc.perform(get("/api/genres")).andExpect(jsonPath("$", org.hamcrest.Matchers.hasItem("Indian Writing")));
        mvc.perform(get("/api/books/13")).andExpect(jsonPath("$.price").value(799.00));
        mvc.perform(get("/api/books/9999")).andExpect(status().isNotFound());
    }

    @Test
    void cartAndOrdersNeedAnAccountAndAdminNeedsTheRole() throws Exception {
        mvc.perform(get("/api/cart")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/stats").header("Authorization", customer())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/stats").header("Authorization", admin())).andExpect(status().isOk());
    }

    @Test
    void fullPurchaseFlowWithStockAndCancellation() throws Exception {
        long bookId = createBook("IT-" + (System.nanoTime() % 100000), 3);
        String auth = customer();

        JsonNode cart = setQty(auth, bookId, 2);
        org.assertj.core.api.Assertions.assertThat(cart.get("subtotal").asDouble()).isEqualTo(600.0);
        org.assertj.core.api.Assertions.assertThat(cart.get("shipping").asDouble()).isEqualTo(0.0);
        mvc.perform(put("/api/cart/items/" + bookId).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":4}"))
                .andExpect(status().isConflict());

        String created = mvc.perform(post("/api/orders").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(ADDRESS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.total").value(600.00))
                .andReturn().getResponse().getContentAsString();
        long orderId = json.readTree(created).get("id").asLong();

        mvc.perform(get("/api/cart").header("Authorization", auth)).andExpect(jsonPath("$.items", hasSize(0)));
        mvc.perform(get("/api/books/" + bookId)).andExpect(jsonPath("$.stock").value(1));

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(get("/api/books/" + bookId)).andExpect(jsonPath("$.stock").value(3));
        mvc.perform(get("/api/orders").header("Authorization", auth)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void lastCopyCanOnlyBeSoldOnce() throws Exception {
        long bookId = createBook("LC-" + (System.nanoTime() % 100000), 1);
        String first = customer();
        String second = customer();
        setQty(first, bookId, 1);
        setQty(second, bookId, 1);
        mvc.perform(post("/api/orders").header("Authorization", first)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(ADDRESS)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/orders").header("Authorization", second)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(ADDRESS)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("just sold out")));
    }

    @Test
    void checkoutValidatesTheAddress() throws Exception {
        String auth = customer();
        setQty(auth, 11, 1);
        mvc.perform(post("/api/orders").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "", "phone", "12345", "address", "x",
                                "city", "Pune", "pincode", "0110"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.phone").exists())
                .andExpect(jsonPath("$.fields.pincode").exists());
    }

    @Test
    void adminManagesOrdersAndBooks() throws Exception {
        String admin = admin();
        long bookId = createBook("AD-" + (System.nanoTime() % 100000), 5);
        String auth = customer();
        setQty(auth, bookId, 1);
        long orderId = json.readTree(mvc.perform(post("/api/orders").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(ADDRESS)))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/admin/orders/" + orderId + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/orders/" + orderId + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        mvc.perform(post("/api/orders/" + orderId + "/cancel").header("Authorization", auth))
                .andExpect(status().isConflict());

        // ordered books are retired, not deleted
        mvc.perform(delete("/api/admin/books/" + bookId).header("Authorization", admin))
                .andExpect(jsonPath("$.result").value("retired"));
        mvc.perform(get("/api/books/" + bookId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", auth))
                .andExpect(jsonPath("$.items[0].title").value(org.hamcrest.Matchers.startsWith("Test Book")));

        long unused = createBook("UN-" + (System.nanoTime() % 100000), 1);
        mvc.perform(delete("/api/admin/books/" + unused).header("Authorization", admin))
                .andExpect(jsonPath("$.result").value("deleted"));

        mvc.perform(get("/api/admin/stats").header("Authorization", admin))
                .andExpect(jsonPath("$.ordersByStatus.SHIPPED").exists());
    }

    @Test
    void staleAdminEditIsRejected() throws Exception {
        String admin = admin();
        long bookId = createBook("ED-" + (System.nanoTime() % 100000), 2);
        Map<String, Object> edit = Map.of("sku", "ED-X" + bookId, "title", "Edited", "author", "Tester",
                "genre", "Testing", "price", "350.00", "stock", 2, "version", 0);
        mvc.perform(put("/api/admin/books/" + bookId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(edit)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/admin/books/" + bookId).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(edit)))
                .andExpect(status().isConflict());
    }
}
