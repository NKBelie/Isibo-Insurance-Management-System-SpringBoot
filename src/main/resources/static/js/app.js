document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.getElementById('sidebar');
    const toggleButton = document.querySelector('.sidebar-toggle');
    const closeButton = document.querySelector('.sidebar-close');

    if (toggleButton) {
        toggleButton.addEventListener('click', () => {
            if (sidebar) {
                const isOpen = sidebar.classList.toggle('open');
                toggleButton.setAttribute('aria-expanded', String(isOpen));
                toggleButton.setAttribute('aria-label', isOpen ? 'Close navigation' : 'Open navigation');
            }
        });
    }

    if (closeButton && sidebar) {
        closeButton.addEventListener('click', () => {
            sidebar.classList.remove('open');
            if (toggleButton) {
                toggleButton.setAttribute('aria-expanded', 'false');
                toggleButton.setAttribute('aria-label', 'Open navigation');
                toggleButton.focus();
            }
        });
    }

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && sidebar && sidebar.classList.contains('open')) {
            sidebar.classList.remove('open');
            if (toggleButton) {
                toggleButton.setAttribute('aria-expanded', 'false');
                toggleButton.setAttribute('aria-label', 'Open navigation');
                toggleButton.focus();
            }
        }
    });

    document.querySelectorAll('[data-confirm-delete]').forEach((button) => {
        button.addEventListener('click', (event) => {
            event.preventDefault();
            const form = button.closest('form');
            if (!form) return;

            const message = button.dataset.confirmDelete || 'This action cannot be undone.';
            if (window.confirm(message)) {
                form.submit();
            }
        });
    });
});
