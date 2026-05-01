document.addEventListener("DOMContentLoaded", function () {
    console.log("Liquor Inventory System Loaded");

    showCurrentDate();
});

function showCurrentDate() {
    const dateElement = document.getElementById("currentDate");

    if (dateElement) {
        const today = new Date();
        dateElement.innerHTML = today.toDateString();
    }
}