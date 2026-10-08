document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.querySelector('#insurance-plan-search');
    const statusFilter = document.querySelector('#insurance-plan-status-filter');

    if (!searchInput && !statusFilter) return;

    const rows = Array.from(document.querySelectorAll('[data-insurance-plan-row]'));

    function applyPlanFilter() {
        const keyword = (searchInput ? searchInput.value : '').trim().toLowerCase();
        const status = statusFilter ? statusFilter.value : '';

        rows.forEach((row) => {
            const text = row.textContent.toLowerCase();
            const rowStatus = row.dataset.status || '';
            const matchesKeyword = !keyword || text.includes(keyword);
            const matchesStatus = !status || rowStatus === status;
            row.style.display = matchesKeyword && matchesStatus ? '' : 'none';
        });
    }

    if (searchInput) searchInput.addEventListener('input', applyPlanFilter);
    if (statusFilter) statusFilter.addEventListener('change', applyPlanFilter);
});
