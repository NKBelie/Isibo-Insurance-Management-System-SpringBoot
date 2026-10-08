document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.getElementById('sidebar');
    const toggleButton = document.querySelector('.sidebar-toggle');
    const closeButton = document.querySelector('.sidebar-close');

    if (toggleButton) {
        toggleButton.addEventListener('click', () => {
            if (sidebar) {
                sidebar.classList.toggle('open');
            }
        });
    }

    if (closeButton && sidebar) {
        closeButton.addEventListener('click', () => {
            sidebar.classList.remove('open');
        });
    }

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
