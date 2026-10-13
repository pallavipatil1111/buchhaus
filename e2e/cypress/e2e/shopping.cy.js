describe("Shopping", () => {
  it("registers through the form", () => {
    cy.visit("/#/login");
    cy.dataCy("to-register").click();
    cy.get('input[name="name"]').type("New Reader");
    cy.get('input[name="email"]').type(`form-${Date.now()}@example.com`);
    cy.get('input[name="password"]').type("short");
    cy.dataCy("auth-submit").click();
    cy.get("#view .form-error").should("contain", "password");
    cy.get('input[name="password"]').clear().type("secret123");
    cy.dataCy("auth-submit").click();
    cy.contains("h1", "Find your next favourite book");
    cy.get("#orders-link").should("be.visible");
  });

  it("adds to cart, earns free delivery, checks out and cancels", () => {
    cy.registerFresh().then(({ token }) => {
      cy.visitAs("/", token);
      cy.dataCy("search").type("Ikigai{enter}");
      cy.dataCy("book-card").first().find('[data-cy="add-to-cart"]').click();
      cy.dataCy("toast").should("contain", "Ikigai");
      cy.dataCy("cart-count").should("have.text", "1");

      cy.dataCy("cart-link").click();
      cy.dataCy("cart-line").should("have.length", 1);
      cy.dataCy("subtotal").should("contain", "₹350");
      cy.dataCy("shipping").should("contain", "₹49");
      cy.contains("Add ₹149 more for free delivery");

      cy.dataCy("qty-plus").click();
      cy.dataCy("qty").should("have.text", "2");
      cy.dataCy("shipping").should("have.text", "Free");
      cy.dataCy("total").should("contain", "₹700");

      cy.dataCy("checkout").click();
      cy.get('input[name="phone"]').type("12345");
      cy.get('input[name="address"]').type("Flat 12, Baner Road");
      cy.get('input[name="city"]').type("Pune");
      cy.get('input[name="pincode"]').type("411045");
      cy.dataCy("place-order").click();
      cy.get("#view .form-error").should("contain", "10-digit");
      cy.get('input[name="phone"]').clear().type("9876543210");
      cy.dataCy("place-order").click();

      cy.dataCy("order-confirmed").should("contain", "is placed");
      cy.dataCy("cart-count").should("have.text", "0");
      cy.dataCy("order").first().within(() => {
        cy.dataCy("status").should("have.text", "PLACED");
        cy.contains("Ikigai × 2");
      });
      cy.on("window:confirm", () => true);
      cy.dataCy("cancel-order").first().click();
      cy.dataCy("order").first().find('[data-cy="status"]').should("have.text", "CANCELLED");
    });
  });

  it("will not let the cart exceed the stock", () => {
    cy.registerFresh().then(({ token }) => {
      cy.visitAs("/", token);
      cy.dataCy("search").type("Introduction to Algorithms{enter}");
      cy.dataCy("book-card").first().find("h3").click();
      for (let i = 0; i < 12; i++) cy.get('[aria-label="More"]').click();
      cy.dataCy("add-to-cart").click();
      cy.dataCy("cart-count").invoke("text").then((n) => expect(Number(n)).to.be.at.most(4));
    });
  });

  it("keeps guests out of the cart and orders", () => {
    cy.visit("/#/cart");
    cy.location("hash").should("eq", "#/login");
  });
});
