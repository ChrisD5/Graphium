const elems = document.querySelectorAll("[data-collapse-toggle]")
elems.forEach(elem => {
    const target = document.getElementById(elem.attributes['data-collapse-toggle'].value);
    elem.addEventListener('click', () => {
        const classes = target.classList;
        if (classes.contains('hidden')) {
            classes.remove('hidden');
        } else {
            classes.add('hidden');
        }
    })
})
