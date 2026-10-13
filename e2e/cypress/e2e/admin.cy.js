describe("Admin", () => {
  const sku = `CY-${Date.now() % 1e6}`;

  beforeEach(() => {
    cy.loginAs("admin@buchhaus.local", "Admin@12345").then((token) => cy.visitAs("/#/admin", token));
  });

  it("shows store statistics", () => {
    cy.contains("h1", "Store admin");
    cy.dataCy("titles").invoke("text").then((t) => expect(Number(t)).to.be.greaterThan(20));
    cy.dataCy("revenue").should("contain", "₹");
  });

  it("adds a book that then appears in the shop, and edits it", () => {
    cy.dataCy("add-book").click();
    cy.dataCy("f-sku").type(sku);
    cy.dataCy("f-title").type(`Cypress in Action ${sku}`);
    cy.dataCy("f-author").type("Test Author");
    cy.dataCy("f-genre").type("Graphic Novels");
    cy.dataCy("f-price").type("555");
    cy.dataCy("f-stock").type("7");
    cy.dataCy("f-save").click();
    cy.dataCy("toast").should("contain", "Book added");
    cy.contains('[data-cy="book-row"]', sku).within(() => cy.dataCy("edit-book").click());
    cy.dataCy("f-price").clear().type("600");
    cy.dataCy("f-save").click();
    cy.contains('[data-cy="book-row"]', sku).should("contain", "₹600");

    cy.visit("/");
    cy.dataCy("search").type(`Cypress in Action ${sku}{enter}`);
    cy.dataCy("book-card").should("contain", "₹600");
  });

  it("moves an order through its life cycle", () => {
    cy.registerFresh().then(({ token }) => {
      cy.request({ method: "PUT", url: "/api/cart/items/2", headers: { Authorization: `Bearer ${token}` }, body: { quantity: 1 } });
      cy.request({ method: "POST", url: "/api/orders", headers: { Authorization: `Bearer ${token}` },
        body: { name: "Cy", phone: "9876543210", address: "Lane 1", city: "Pune", pincode: "411001" } })
        .its("body.id").then((id) => {
          cy.dataCy("tab-orders").click();
          cy.get(`[data-cy="order"][data-id="${id}"]`).find('[data-cy="order-status"]').select("SHIPPED");
          cy.get(`[data-cy="order"][data-id="${id}"]`).find('[data-cy="status"]').should("have.text", "SHIPPED");
          cy.get(`[data-cy="order"][data-id="${id}"]`).find('[data-cy="order-status"]').select("DELIVERED");
          cy.get(`[data-cy="order"][data-id="${id}"]`).find('[data-cy="status"]').should("have.text", "DELIVERED");
        });
    });
  });

});

// Outside the "Admin" block on purpose: its beforeEach opens /#/admin as the admin, and Cypress does
// not reload the page when cy.visit only repeats the same URL, so the customer token would never load.
describe("Admin pages for customers", () => {
  it("are hidden from customers", () => {
    cy.registerFresh().then(({ token }) => {
      cy.visitAs("/#/admin", token);
      cy.contains("Admins only.");
      cy.dataCy("admin-link").should("not.be.visible");
    });
  });
});
