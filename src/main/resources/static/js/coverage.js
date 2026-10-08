document.addEventListener('DOMContentLoaded', () => {
    const planSelect = document.querySelector('#coverage-insurance-plan');
    const amountInput = document.querySelector('#coverage-amount');
    const validationBox = document.querySelector('#coverage-validation');
    const existingValue = document.querySelector('#existing-coverage-value');
    const coverageLimitValue = document.querySelector('#plan-coverage-limit');
    const remainingValue = document.querySelector('#remaining-coverage');

    if (!planSelect) return;

    const planMap = {}; 
    document.querySelectorAll('[data-plan-info]').forEach((item) => {
        const id = item.dataset.planInfo;
        const limit = Number(item.dataset.planLimit || 0);
        const used = Number(item.dataset.planUsed || 0);
        planMap[id] = { limit, used };
    });

    function updateCoverageSummary() {
        const planId = planSelect.value;
        const amount = Number(amountInput ? amountInput.value : 0);
        const plan = planMap[planId];

        if (!plan) {
            if (coverageLimitValue) coverageLimitValue.textContent = '—';
            if (existingValue) existingValue.textContent = '—';
            if (remainingValue) remainingValue.textContent = '—';
            return;
        }

        const remaining = Math.max(plan.limit - plan.used, 0);
        if (coverageLimitValue) coverageLimitValue.textContent = formatCurrency(plan.limit);
        if (existingValue) existingValue.textContent = formatCurrency(plan.used);
        if (remainingValue) remainingValue.textContent = formatCurrency(remaining);

        if (validationBox) {
            const newRemaining = remaining - amount;
            if (plan.limit > 0 && amount > 0 && newRemaining < 0) {
                validationBox.classList.add('visible');
                validationBox.textContent = 'Total coverage exceeds the insurance plan limit.';
            } else {
                validationBox.classList.remove('visible');
                validationBox.textContent = '';
            }
        }
    }

    if (amountInput) {
        amountInput.addEventListener('input', updateCoverageSummary);
    }

    planSelect.addEventListener('change', updateCoverageSummary);
    updateCoverageSummary();

    function formatCurrency(value) {
        return new Intl.NumberFormat('en-US', {
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        }).format(value) + ' RWF';
    }
});
