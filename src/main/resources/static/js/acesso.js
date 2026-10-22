"use strict";

const form = document.querySelector("[data-acesso]");
if (form) {
    const senha = form.elements.namedItem("senha");
    const confirmacao = form.elements.namedItem("confirmacao");
    const nome = form.elements.namedItem("nome");
    const feedback = form.querySelector(".auth-feedback");

    function validar() {
        if (nome) {
            nome.setCustomValidity(nome.value.trim() ? "" : "Informe seu nome.");
        }
        if (confirmacao) {
            confirmacao.setCustomValidity(
                confirmacao.value === senha.value ? "" : "As senhas precisam ser iguais."
            );
        }
    }

    form.addEventListener("input", () => {
        validar();
        feedback.hidden = true;
    });

    form.addEventListener("submit", (event) => {
        event.preventDefault();
        validar();
        if (!form.reportValidity()) return;

        feedback.textContent = form.dataset.acesso === "registro"
            ? "O cadastro com senha ainda não foi integrado. Seus dados não foram enviados nem salvos."
            : "A autenticação ainda não foi integrada. Use o link abaixo para explorar o protótipo.";
        feedback.hidden = false;
    });
}
