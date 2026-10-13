describe("Catalogue", () => {
  beforeEach(() => cy.visit("/"));

  it("shows the first page of books with prices and stock", () => {
    cy.dataCy("book-card").should("have.length", 12);
    cy.dataCy("result-count").invoke("text").then((t) => expect(parseInt(t, 10)).to.be.at.least(24));
    cy.dataCy("page").should("contain", "Page 1 of");
  });

  it("filters by genre and sorts by price", () => {
    cy.contains('[data-cy="genre"]', "Technology").click();
    cy.dataCy("result-count").should("have.text", "6 books");
    cy.dataCy("sort").select("price_desc");
    cy.dataCy("book-card").first().should("contain", "Introduction to Algorithms").and("contain", "₹1,499");
  });

  it("searches by author", () => {
    cy.dataCy("search").type("kalam{enter}");
    cy.contains("h1", "Results for “kalam”");
    cy.dataCy("book-card").should("have.length", 1).first().should("contain", "Wings of Fire");
  });

  it("marks sold-out books and blocks buying them", () => {
    cy.dataCy("search").type("five point{enter}");
    cy.dataCy("book-card").first().within(() => {
      cy.contains("Out of stock");
      cy.dataCy("add-to-cart").should("be.disabled").and("have.text", "Sold out");
    });
  });

  it("opens a book page", () => {
    cy.dataCy("search").type("Malgudi{enter}");
    cy.dataCy("book-card").first().find("h3").click();
    cy.dataCy("book-title").should("have.text", "Malgudi Days");
    cy.dataCy("book-price").should("contain", "₹250");
    cy.contains("R. K. Narayan");
  });

  it("asks guests to sign in before adding to the cart", () => {
    cy.dataCy("book-card").first().find('[data-cy="add-to-cart"]').click();
    cy.location("hash").should("eq", "#/login");
  });
});
