"use strict";
const form = document.querySelector("[data-registro]");
if (form) {
    const senha = form.elements.namedItem("senha");
    const confirmacao = form.elements.namedItem("confirmacao");
    const validar = () => confirmacao.setCustomValidity(
        senha.value === confirmacao.value ? "" : "As senhas precisam ser iguais."
    );
    form.addEventListener("input", validar);
    form.addEventListener("submit", (event) => {
        validar();
        if (!form.reportValidity()) event.preventDefault();
    });
}
