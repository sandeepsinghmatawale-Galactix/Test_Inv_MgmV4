function calculateWellSale(row) {
    let opening =
        parseInt(row.querySelector(".opening").value) || 0;

    let received =
        parseInt(row.querySelector(".received").value) || 0;

    let closing =
        parseInt(row.querySelector(".closing").value) || 0;

    let totalAvailable = opening + received;

    if (closing > totalAvailable) {
        alert("Invalid closing stock");
        row.querySelector(".closing").value = "";
        row.querySelector(".sale").value = "";
        return;
    }

    let sale = totalAvailable - closing;

    row.querySelector(".sale").value = sale;
}