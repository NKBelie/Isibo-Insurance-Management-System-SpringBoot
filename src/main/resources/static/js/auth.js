document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-password-toggle]").forEach((toggle) => {
        toggle.addEventListener("click", () => {
            const input = document.getElementById(toggle.dataset.passwordToggle);
            if (!input) return;

            const reveal = input.type === "password";
            input.type = reveal ? "text" : "password";
            toggle.setAttribute("aria-pressed", String(reveal));
            toggle.setAttribute("aria-label", `${reveal ? "Hide" : "Show"} ${input.id === "passwordConfirmation" ? "confirmation " : ""}password`);
        });
    });

    const registrationForm = document.querySelector("[data-register-form]");
    if (registrationForm) {
        const password = registrationForm.querySelector("#password");
        const confirmation = registrationForm.querySelector("#passwordConfirmation");
        const message = registrationForm.querySelector("#password-match-message");

        const validateMatch = () => {
            if (!confirmation.value) {
                confirmation.setCustomValidity("");
                confirmation.removeAttribute("aria-invalid");
                message.textContent = "";
                message.classList.remove("match-error");
                return;
            }

            const matches = password.value === confirmation.value;
            confirmation.setCustomValidity(matches ? "" : "Passwords do not match.");
            confirmation.setAttribute("aria-invalid", String(!matches));
            message.textContent = matches ? "Passwords match." : "Passwords do not match.";
            message.classList.toggle("match-error", !matches);
        };

        password.addEventListener("input", validateMatch);
        confirmation.addEventListener("input", validateMatch);
    }

    document.querySelectorAll("[data-auth-form]").forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (!form.reportValidity()) {
                event.preventDefault();
                return;
            }

            const submit = form.querySelector("[type=submit]");
            if (!submit) return;

            submit.disabled = true;
            submit.setAttribute("aria-busy", "true");
            submit.textContent = submit.dataset.submitLabel || "Please wait…";
        });
    });
});
