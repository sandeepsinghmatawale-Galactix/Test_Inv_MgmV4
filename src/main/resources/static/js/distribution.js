function validateDistribution() {
    let rows = document.querySelectorAll(".distribution-row");

    let brandTotals = {};

    rows.forEach(row => {
        let brandId =
            row.querySelector(".brandId").value;

        let qty =
            parseInt(
                row.querySelector(".distributedQty").value
            ) || 0;

        if (!brandTotals[brandId]) {
            brandTotals[brandId] = 0;
        }

        brandTotals[brandId] += qty;
    });

    console.log("Distribution Totals:", brandTotals);
}