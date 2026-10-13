// Shared commands. Logging in through the API keeps specs fast; the login *form* has its own test.
const TOKEN_KEY = "buchhaus.token";

Cypress.Commands.add("loginAs", (email, password) =>
  cy.request("POST", "/api/auth/login", { email, password }).its("body.token"));

Cypress.Commands.add("registerFresh", () => {
  const email = `cy-${Date.now()}-${Cypress._.random(1e6)}@example.com`;
  return cy.request("POST", "/api/auth/register", { name: "Cypress Reader", email, password: "secret123" })
    .then((res) => ({ email, token: res.body.token }));
});

Cypress.Commands.add("visitAs", (path, token) =>
  cy.visit(path, { onBeforeLoad: (win) => win.localStorage.setItem(TOKEN_KEY, token) }));

Cypress.Commands.add("dataCy", (name) => cy.get(`[data-cy="${name}"]`));
