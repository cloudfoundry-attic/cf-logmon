const timeNodes = Array.from(document.querySelectorAll('time'));
timeNodes.forEach(function (node) {
    const time = new Date(node.innerText);
    node.innerHTML = `${time.toLocaleTimeString()} <br/> ${time.toLocaleDateString()}`
});
